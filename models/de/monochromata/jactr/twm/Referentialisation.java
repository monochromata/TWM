package de.monochromata.jactr.twm;

import static de.monochromata.jactr.twm.ITWM.getObjectVsMethod;
import static de.monochromata.jactr.twm.TWMModule.doLocked;
import static de.monochromata.jactr.twm.TWMModule.getFeaturesSlot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.buffer.IActivationBuffer;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunk.ISymbolicChunk;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.module.declarative.IDeclarativeModule;
import org.jactr.core.module.retrieval.IRetrievalModule;
import org.jactr.core.production.request.ChunkRequest;
import org.jactr.core.production.request.ChunkTypeRequest;
import org.jactr.core.production.request.IRequest;
import org.jactr.core.runtime.ACTRRuntime;
import org.jactr.core.slot.IMutableSlot;
import org.jactr.core.slot.ISlot;
import org.jactr.io.antlr3.parser.lisp.LispParser.addDm_return;
import org.jactr.tools.misc.ChunkUtilities;

import de.monochromata.jactr.dm.INonMergingDeclarativeModule;
import de.monochromata.jactr.rm.IScopedRetrievalModule;
import de.monochromata.jactr.tls.CollectionSlot;
import de.monochromata.jactr.tls.ConceptualSchema;
import de.monochromata.jactr.tls.ICollectionSlot;
import de.monochromata.jactr.tls.ReferencePotential;
import de.monochromata.jactr.tls.Scope;
import de.monochromata.jactr.tls.SymbolicChunk;
import de.monochromata.jactr.twm.WordActivation.CompleteRequestTimedEvent;
import de.monochromata.jactr.twm.WordActivation.WordData;

/**
 * Instantiates and re-activates TWM nodes when handed {@link ReferencePotential}
 * instances. Also adds requested {@link ReferencePotential} instances to the
 * {@link TWMGraphemicBuffer} to cause them to spread activation.
 */
public class Referentialisation extends WordActivation {

	private static final transient Log LOGGER = LogFactory.getLog(Referentialisation.class); 
	protected final IChunkType referencePotentialChunkType;
	
	public Referentialisation(TWMModule module, TWMBuffer buffer,
			TWMGraphemicBuffer graphemicBuffer)
			throws InterruptedException, ExecutionException {
		super(module, buffer, graphemicBuffer);
		this.referencePotentialChunkType = module.getReferencePotentialChunkType();
	}

	/**
	 * Only {@link ChunkRequest}s are accepted, because the meta-data
	 * provided by REMMA is required to operate the {@link TWMModule}.
	 */
	@Override
	public boolean willAccept(IRequest request) {
		return request != null
				&& request instanceof ChunkRequest
				&& ((ChunkRequest)request).getChunkType().isA(referencePotentialChunkType);
	}

	@Override
	protected Object startRequest(IRequest request, IActivationBuffer buffer,
			double requestTime) {

		// Ensure that the request chunk is the last element in the buffer
		IChunk refPot = ((ChunkRequest)request).getChunk();
		if(!buffer.getSourceChunks().contains(refPot)) {
			// Ensure that the request chunk spreads activation because it
			// might not be contained in another buffer.
			buffer.addSourceChunk(refPot);
		}
	
		module.fireRequestReferentialisation(requestTime, refPot);
		
		setBusy(buffer);
		return new RequestData(refPot);
	}
	
	// abortRequest(...) is inherited
	
	// postStart(...) in super class sets requestData.startTime

	protected void prepareFinish(IRequest request, IActivationBuffer buffer,
			RequestData data) {
		// Make sure a non-merging declarative module is used
		INonMergingDeclarativeModule dm = (INonMergingDeclarativeModule)
				module.getModel().getDeclarativeModule();
		
		ISymbolicChunk sc = data.requestChunk.getSymbolicChunk();
		boolean isDefinite = (boolean)sc.getSlot("isDefinite").getValue();
		String typeSchemaId = (String)data.requestChunk.getMetaData(ITWM.schema);
		Scope scope = (Scope)data.requestChunk.getMetaData(ITWM.scope);
		data.referent = (IChunk)data.requestChunk.getMetaData(ITWM.referent);
		data.coReferenceChain = (String)data.requestChunk.getMetaData(ITWM.coReferenceChain);
		data.declaredIn = (String)data.requestChunk.getMetaData(ITWM.declaredIn);
		data.roleIn = (String)data.requestChunk.getMetaData(ITWM.roleIn);
		data.roleId = (String)data.requestChunk.getMetaData(ITWM.roleId);
		data.returnId = (String)data.requestChunk.getMetaData(ITWM.returnId);
		
		// Directly activate the schema that declares the reference potential, if
		// this connection is marked in the meta data. This is a simplified
		// form of concept formation. The effects of this activation should be immediate.
		ensureTypeSchemaExistsIfNonNull(dm, data, typeSchemaId);
		ensureTypeSchemaExistsIfNonNull(dm, data, data.declaredIn);
		
		if(isDefinite) {
			// Definite: confusion for tokens other than the one created from
			// the given reference potential is plausible
			if(data.typeSchema != null) {
				throw new IllegalStateException("the meta data entry 'schema'"
						+ " may only be used in indefinite reference potentials, not in"
						+ " definite ones like "+data.requestChunk);
			}
			if(data.declaredIn != null) {
				throw new IllegalStateException("the meta data entry 'declaredIn'"
						+ " may only be used in indefinite reference potentials"
						+ " that belong to callable invocations, not in"
						+ " definite ones like "+data.requestChunk);
			}

			data.typeSchema = getBestMatchingType(sc, scope);
			data.referent = getBestMatchingToken(sc, scope);
			if(data.referent != null && !data.referent.getSymbolicChunk().getName().equals("error")) {
				// Re-Activate best matching token
				data.finishActions.add( rd-> {
					reActivate(dm, isDefinite, rd.requestChunk, rd.referent,
							rd.coReferenceChain, rd.declaredIn, rd.roleIn, rd);
					specify(rd.referent, rd.typeSchema, rd);
				});
				data.completionDurationSuppliers.add(rm -> {
					return schemaAccessDuration(rm, data.typeSchema)
							+ schemaAccessDuration(rm, data.referent);
				});
			} else {
				// Instantiate type schema (the referent will not be taken
				// from the co-reference chain - it was not retrieved above
				// it might thus be outdated, but will saved to it).
				data.finishActions.add( rd-> {
					rd.referent = instantiateTypeSchemaAndPerformPostProcessing(
						rd.requestChunk, rd.typeSchema, rd.coReferenceChain,
						rd.declaredIn, rd.roleIn, rd);
				});
				data.completionDurationSuppliers.add(rm -> {
					return schemaAccessDuration(rm, data.typeSchema)
							+ schemaCreationDuration(); // data.referent
				});
			}
		} else {
			// Indefinite: the model should not permit confusion for tokens not created
			// from the given reference potential
			if(data.referent != null) {
				// Re-Activate the referent, do not search for matching tokens in general
				data.finishActions.add( rd -> {
					reActivate(dm, isDefinite, rd.requestChunk, rd.referent,
							rd.coReferenceChain, rd.declaredIn, rd.roleIn, rd);
				});
				data.completionDurationSuppliers.add(rm -> {
					return schemaAccessDuration(rm, data.referent);
				});
				// TODO: Shouldn't there be a type schema that gets another
				// access (it might need to be retrieved)?
			} else {
				// Instantiate the referent and save it
				// If schema != null: use that (applies to field, method and supertype declarations), or
				
				// If there is a declaredIn attribute, it refers to a type schema
				// (this case applies to invocations of callables).
				if(data.typeSchema == null && data.declaredIn != null) {
					try {
						data.typeSchema = dm.getChunk(data.declaredIn).get();
					} catch (InterruptedException | ExecutionException e) {
						LOGGER.error("Failed to get type schema declaredIn="+data.declaredIn
								+" mentioned in reference potential "+data.requestChunk
								+" from declarative memory: "+e.getMessage(), e);
					}
					data.completionDurationSuppliers.add(rm -> {
						return schemaAccessDuration(rm, data.typeSchema);
					});
				}
				
				// If the reference potential does not belong to the invocation
				// of a callable (i.e. invocations of a callable refer to a
				// method schema but declare co-reference for the return value
				// and can be identified by the declaredIn attribute they declare),
				// use the referent of the co-reference chain, if there
				// is a co-reference chain and it has a referent
				if(data.declaredIn == null && data.coReferenceChain != null) {
					data.referent = getReferentOfCoReferenceChain(data.coReferenceChain);
					if(data.referent != null) {
						data.finishActions.add( rd -> {
							reActivate(dm, isDefinite, rd.requestChunk, rd.referent,
									rd.coReferenceChain, rd.declaredIn, rd.roleIn, rd);
							if(rd.typeSchema != null)
								specify(rd.referent, rd.typeSchema, rd);
						});
						// TODO: Shouldn't there be a type schema that gets another
						// access (it might need to be retrieved)?
						data.completionDurationSuppliers.add(rm -> {
							return (data.typeSchema==null?
										0.0:schemaAccessDuration(rm, data.typeSchema))
									+ schemaAccessDuration(rm, data.referent);
						});
					}
				} // else if(declaredIn != null): the reference potential belongs
				// to the invocation of a callable and co-reference with its return
				// value will be handled during instantiation of the method type schema.
				
				if(data.referent == null) {
					if(data.typeSchema == null) {
						data.typeSchema = getBestMatchingType(sc, scope);
						data.completionDurationSuppliers.add(rm -> {
							return schemaAccessDuration(rm, data.typeSchema);
						});
					}
					data.finishActions.add( rd -> {
						rd.referent = instantiateTypeSchemaAndPerformPostProcessing(
								rd.requestChunk, rd.typeSchema, rd.coReferenceChain,
								rd.declaredIn, rd.roleIn, rd);
					});
					data.completionDurationSuppliers.add(rm -> {
						return schemaCreationDuration(); // data.referent
					});
				}
			}
		}

		data.finishActions.add( rd -> {
			TWMModule.doLocked(rd.requestChunk, c -> {
				c.setMetaData(ITWM.referent, rd.referent);
			});
		});
	}
	
	protected void ensureTypeSchemaExistsIfNonNull(INonMergingDeclarativeModule dm,
			RequestData data, String typeSchemaId) {
		if(typeSchemaId != null) {
			try {
				data.typeSchema = dm.getChunk(typeSchemaId).get();
				if(data.typeSchema == null) {
					data.typeSchema = createTypeSchemaAndAddItToDM(typeSchemaId);
					data.completionDurationSuppliers.add(rm -> {
						return schemaCreationDuration(); // data.typeSchema
					});
				}
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error("Failed to get type schema schema="+typeSchemaId
						+" mentioned in reference potential "+data.requestChunk
						+" from declarative memory: "+e.getMessage(), e);
			}
		}
	}
	
	@Override
	protected void finishRequest(IRequest request, IActivationBuffer buffer,
			Object startValue) {
		RequestData requestData = (RequestData)startValue;
		
		// Prepare finish
		prepareFinish(request, buffer, requestData);
		
		// Compute completion duration
		IScopedRetrievalModule retrievalModule = (IScopedRetrievalModule)
				module.getModel().getModule(IScopedRetrievalModule.class);
		double completionDuration = 0.0;
		for(Function<IScopedRetrievalModule,Double> supplier: requestData.completionDurationSuppliers) {
			completionDuration += supplier.apply(retrievalModule);
		}
		requestData.endTime = requestData.startTime + completionDuration;
		
		// Queue request completion
		CompleteRequestTimedEvent event =
				new CompleteRequestTimedEvent(requestData.startTime, requestData.endTime,
						request, buffer, requestData);
		setCurrentTimedEvent(event);
		buffer.getModel().getTimedEventQueue().enqueue(event);
	}
	
	@Override
	protected void completeRequest(IRequest request, IActivationBuffer buffer,
			WordData startValue) {
		RequestData requestData = (RequestData)startValue;
		
		// Perform finish actions (i.e. actually execute the request)
		for(Consumer<RequestData> action: requestData.finishActions) {
			action.accept(requestData);
		}
		
		// Stop graphemic information from spreading activation
		graphemicBuffer.clear();
		
		// All token instantiated during the processing of this
		// request have been added to DM already, but use the
		// referent token to replace the reference potential that
		// initiated this request.
		buffer.removeSourceChunk(requestData.requestChunk);
		buffer.addSourceChunk(requestData.referent);
		setFree(buffer);
		
		access("referent", requestData.referent, requestData);
		if(requestData.typeSchema != null)
			access("type schema", requestData.typeSchema, requestData);
	}
	
	private void access(String kindOfChunk, IChunk chunk, RequestData rd) {
		module.fireAccess(kindOfChunk, chunk, rd.startTime, rd.endTime, rd.requestChunk);
		if(chunk != null) {
			doLocked(chunk, c -> {
				c.getSubsymbolicChunk().accessed(rd.endTime);
			});
		}
	}

	/**
	 * Creates the type schema identified by the given ID and adds it to
	 * declarative memory.
	 * 
	 * @param typeSchemaId
	 * @return
	 */
	protected IChunk createTypeSchemaAndAddItToDM(String typeSchemaId) {
		return module.createConceptualSchemaAndDependentSchemataAndAddThemToDM(typeSchemaId);
	}

	/**
	 * Copies all features of the feature provider (type or token schema)
	 * that are not yet contained in the first referent into the
	 * feature slot of the first referent.
	 * 
	 * @param featureReceiver
	 * @param featureProvider
	 * @see ICollectionSlot#difference(ICollectionSlot)
	 */
	protected void specify(IChunk featureReceiver, IChunk featureProvider, RequestData rd) {
		module.fireSpecify(featureReceiver, featureProvider, rd.startTime, rd.endTime, rd.requestChunk);
		
		if(!featureReceiver.getSymbolicChunk().getName().equals("error")
			&& !featureProvider.getSymbolicChunk().getName().equals("error")) {
		
			// Make sure that methods and objects do not get mixed
			String receiverOvM = getObjectVsMethod(featureReceiver);
			String providerOvM = getObjectVsMethod(featureProvider);
			if(receiverOvM.equals(providerOvM)) {
				// Get features
				IDeclarativeModule dm = module.getModel().getDeclarativeModule();
				ICollectionSlot referentFeatures = TWMModule.getFeaturesSlot(featureReceiver);
				ICollectionSlot typeFeatures = TWMModule.getFeaturesSlot(featureProvider);
				
				// TODO: For those features that are contained in the referent
				// already, the activation from the type may be "copied", the
				// may of the two may be used, or so.
				
				Collection<Object> difference = typeFeatures.difference(referentFeatures);
				for(Object object: difference) {
					copyFeatureAndAddItToReferentAndDM(dm, object, featureProvider,
							featureReceiver, referentFeatures, false);
				}
			}
		}
	}
	
	private void copyFeatureAndAddItToReferentAndDM(IDeclarativeModule dm,
			Object featureObj, IChunk featureSource, IChunk featureTarget,
			ICollectionSlot featuresSlot, boolean removeFeatureBeforeAddingCopy) {
		copyFeatureAndAddItToReferentAndDM(dm, featureObj, featureSource,
				featureTarget, featuresSlot, removeFeatureBeforeAddingCopy,
				null);
	}
	
	private void copyFeatureAndAddItToReferentAndDM(IDeclarativeModule dm,
			Object featureObj, IChunk featureSource, IChunk featureTarget,
			ICollectionSlot featuresSlot, boolean removeFeatureBeforeAddingCopy,
			FeaturePassing listener) {
		
		ensureFeatureIsAChunk(featureObj, featureSource, featureTarget, feature -> {
			try {
				// Copy the feature
				IChunk featureCopy = dm.copyChunk(feature).get();
				module.addToDMAndEnsureNameIsUnique(dm, featureCopy);
				TWMModule.doLocked(featureTarget, r -> {
					if(removeFeatureBeforeAddingCopy)
						featuresSlot.remove(feature);
					featuresSlot.add(featureCopy);
				});
				
				// Inform consumer about old and new chunk
				if(listener != null) {
					listener.pass(feature, featureCopy, featuresSlot);
				}
				
				// Recurse for technical features
				if((Boolean)featureCopy.getMetaData(ITWM.isTechnical)) {
					ICollectionSlot featureCopyFeatures = getFeaturesSlot(featureCopy);
					// A copy of the list is required, because the original list will be modified
					for(Object object: new ArrayList<Object>(featureCopyFeatures.getValues())) {
						copyFeatureAndAddItToReferentAndDM(dm, object, feature, featureCopy,
								featureCopyFeatures, removeFeatureBeforeAddingCopy, listener);
					}
				}
			} catch(InterruptedException|ExecutionException e) {
				LOGGER.error("Failed to copy feature="+featureObj+" while specifying referent="
						+featureTarget+" using feature source="+featureSource+": "+e.getMessage(), e);
			}
		});
	}
	
	private <T> T ensureFeatureIsAChunk(Object featureObj, IChunk featureSource,
			Function<IChunk,T> featureConsumer) {
		return ensureFeatureIsAChunk0(featureObj, featureSource, null, featureConsumer);
	}
	
	private void ensureFeatureIsAChunk(Object featureObj, IChunk featureSource, IChunk featureTarget,
			Consumer<IChunk> featureConsumer) {
		ensureFeatureIsAChunk0(featureObj, featureSource, featureTarget,
				feature -> {
					featureConsumer.accept(feature);
					return null;
				});
	}
	
	private <T> T ensureFeatureIsAChunk0(Object featureObj, IChunk featureSource, IChunk featureTarget,
			Function<IChunk,T> featureConsumer) {
		if(featureObj instanceof IChunk) {
			return featureConsumer.apply((IChunk)featureObj);
		} else {
			throw new IllegalStateException("Only chunks may appear as features,"
					+" but "+featureSource+" contains non-chunk feature "+featureObj+"."
					+(featureTarget==null?"":" Cannot copy this feature to "+featureTarget));
		}
	}
	
	/**
	 * Re-activates the existing referent
	 * 
	 * @param referent
	 */
	protected void reActivate(IDeclarativeModule dm, boolean isDefinite,
			IChunk referencePotential, IChunk referent, String coReferenceChain,
			String declaredIn, String roleIn, RequestData data) {
		
		// If the reference potential is definite, it might have a new referent
		// after this request, hence
		// - maintain co-reference chains, and
		// - link arguments to invocations of callables.
		
		if(isDefinite) {
			
			if(coReferenceChain != null) {
				
				// Because co-reference chains are not considered when looking for
				// a suitable referent of the definite reference potential,
				// it is possible that the co-reference chain of the reference potential
				// has a different referent than the one to be re-activated via the
				// reference potential. This might have implications for (1) co-reference
				// chains and (2) said two referents.
				// 
				// (1) It might be possible to merge the two co-reference chains of
				//     the given reference potential and the one that the found referent
				//     belongs to (if the latter co-reference chain exists and differs
				//     from the co-reference chain of the reference potential). It would
				//     then need to be considered, if there are conditions under which
				//     the merged co-reference chains would need to be split up again
				//     (e.g. when the activation of the referent that caused the merge
				//     falls below retrieval threshold). Because of this potential
				//     co-reference chains are not merged currently.
				//
				// (2) The referent of the co-reference chain shall specify the retrieved
				//     referent. (TODO: LATER: why not merge/replace?)
				// 
				// Note: above description does not apply to invocations of callables
				// because they are indefinite and indefinite reference potentials
				// have been excluded above.
				
				IChunk coRefChainReferent = getReferentOfCoReferenceChain(coReferenceChain);
				if(coRefChainReferent != null) {
					specify(referent, coRefChainReferent, data);
				}
			}
			
			// Note that declardedIn is not relevant here, because it is available
			// for indefinite reference potentials (of invocations of callables)
			// only and arguments are only updated for definite reference potentials.
			
			// Because arguments are definite reference potentials, their
			// referent might have changed and would require an update of
			// the corresponding callable invocation.
			try {
				RoleIdInfo change = maybeLinkArgumentToReferentOfCallableInvocation(
						referencePotential.getSymbolicChunk().getName(),
							roleIn, data.roleId, dm, referent, data);
				if(change != null) {
					updateRoleIds(dm, referencePotential, data, Collections.singletonMap(data.roleId, change));
				}
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error("Failed to link re-activated argument "+referent
						+" to callable invocation: "+e.getMessage(), e);
			}
			
		}
				
		// Access to compute base-level will be added in finishRequest(...)
	}
	
	/**
	 * Returns the referent of the identified co-reference chain
	 * 
	 * @param coReferenceChain The ID of the co-reference chain
	 * @return Null, if no reference potential on the co-reference chain has
	 * 	been referentialised, yet.
	 */
	protected IChunk getReferentOfCoReferenceChain(String coReferenceChain) {
		return module.coReferenceChainReferents.get(coReferenceChain);
	}
	
	protected IChunk instantiateTypeSchemaAndPerformPostProcessing(
			IChunk refPot, IChunk typeSchema, String coReferenceChain,
			String declaredIn, String roleIn, RequestData data) {
		
		module.fireInstantiate(typeSchema, data.startTime, data.endTime, data.requestChunk);
		
		// Return null, if the type schema to be instantiated could not be retrieved
		if(typeSchema.getSymbolicChunk().getName().equals("error"))
			return null;
		
		IDeclarativeModule dm = module.getModel().getDeclarativeModule();
		
		IChunk referent = null;
		try {
			
			// If the current reference potential represents a callable invocation,
			// the roleIds of its arguments need to be replaced by the IDs of the
			// copies of the chunks identified by the role IDs. First, the arguments
			// and the old roleIds need to be collected.
			Map<String,RoleIdTranslation> roleIdTranslations =
					maybeCollectArguments(declaredIn, refPot, RoleIdTranslation::new);
			ReturnIdTranslation returnTranslation = maybeCollectReturn(declaredIn,
					refPot, data.returnId, ReturnIdTranslation::new);
			
			// Create a copy so that sub-symbolic information is copied, too and
			// turn the copy into a token.
			referent = dm.copyChunk(typeSchema).get();
			ISlot typeVsTokenSlot = referent.getSymbolicChunk().getSlot("typeVsToken");
			if(!(typeVsTokenSlot instanceof IMutableSlot)) {
				LOGGER.error("Immutable typeVsToken slot: cannot turn copy of type into token: "+referent);
			} else {
				((IMutableSlot)typeVsTokenSlot).setValue("token");
			}
			
			// Copy the features of the type schema and add the feature copies
			// and the referent to declarative memory.
			// Also collect IDs of new roles, if the old roles are copied.
			ICollectionSlot referentFeatures = TWMModule.getFeaturesSlot(referent);
			ICollectionSlot typeFeatures = TWMModule.getFeaturesSlot(typeSchema);
			for(Object object: typeFeatures.getValues()) {
				copyFeatureAndAddItToReferentAndDM(dm, object, typeSchema, referent, referentFeatures, true,
						(feature, featureCopy, featuresSlot) -> {
						
							String oldRoleId = feature.getSymbolicChunk().getName();
							String newRoleId = featureCopy.getSymbolicChunk().getName();
							
							Consumer<ReturnIdInfo> doTranslate = trans -> {
								trans.setRole(featureCopy);
								trans.setRoleId(newRoleId);
								trans.setRoleContainer(featuresSlot);
							};
							
							RoleIdTranslation translation = roleIdTranslations.get(oldRoleId);
							if(translation != null) {
								doTranslate.accept(translation);
							} else if(returnTranslation != null
									&& returnTranslation.getRoleId().equals(oldRoleId)) {
								doTranslate.accept(returnTranslation);
							}
						});
			}
			module.addToDMAndEnsureNameIsUnique(dm, referent);
	
			// Make the referent accessible to all co-referent reference potentials.
			// If this reference potential belongs to the invocation of a
			// callable, make the return value available to the co-reference chain.
			if(coReferenceChain != null) {
				if(declaredIn == null) {
					module.coReferenceChainReferents.put(coReferenceChain, referent);
				} else { // implies: returnTranslation != null
					IChunk coRefChainReferent = getReferentOfCoReferenceChain(coReferenceChain);
					IChunk returnReferent = returnTranslation.getRole();
					if(coRefChainReferent != null) {
						// Specify the existing referent of the co-reference chain with
						// the return value and then replace the return value by the
						// specified referent of the co-reference chain.
						specify(coRefChainReferent, returnReferent, data);
						returnTranslation.setRole(coRefChainReferent);
						returnTranslation.setRoleId(coRefChainReferent.getSymbolicChunk().getName());
						replaceFeature(referent, returnTranslation.getRoleContainer(),
								returnReferent, coRefChainReferent);
					} else {
						// Set return value as new referent of the co-reference chain
						module.coReferenceChainReferents.put(coReferenceChain, returnReferent);
					}
				}
			}
			
			// Rename roleIds in argument reference potentials to the IDs of the
			// copies of the roleIds in the referent
			updateRoleIds(dm, refPot, data, roleIdTranslations);
			// Rename the returnId in the referent to the ID of the copy of the
			// returnId that it contains
			if(returnTranslation != null) {
				updateRoleIds(dm, refPot, data, Collections.singletonMap("", returnTranslation));
			}
			
			// Link the referent of a callable invocation to all of its arguments
			// that have a referent so far ... 
			maybeLinkReferentOfCallableInvocationToArgumentsAndUpdateRoleIds(
					dm, declaredIn, refPot, referent, data, roleIdTranslations);

			// Link argument to method: handle roleIn and roleId
			RoleIdInfo change = maybeLinkArgumentToReferentOfCallableInvocation(
					refPot.getSymbolicChunk().getName(),
					roleIn, data.roleId, dm, referent, data);
			if(change != null)
				updateRoleIds(dm, refPot, data, Collections.singletonMap(data.roleId, change));
			
		} catch(InterruptedException|ExecutionException e) {
			LOGGER.error("Failed to instantiate type schema="+typeSchema
					+" and perform post processing: "+e.getMessage(), e);
		}
		return referent;
	}

	/**
	 * Collect arguments, if the reference potential belongs to the invocation
	 * of a callable (i.e. declaredIn != null).
	 * 
	 * @param declaredIn
	 * @param refPot
	 * @param factory
	 * @return
	 */
	private <T extends RoleIdInfo> Map<String,T> maybeCollectArguments(String declaredIn,
			IChunk refPot, Function<String,T> factory) {
		Map<String,T> roleIdInfos = null;
		if(declaredIn != null) {
			String refPotID = refPot.getSymbolicChunk().getName();
			List<String> argumentReferencePotentialIds = module.arguments.get(refPotID);
			roleIdInfos = new HashMap<String,T>();
			for(String argumentRefPotId: argumentReferencePotentialIds) {
				String argRoleId = module.roleIds.get(argumentRefPotId);
				T info = factory.apply(argumentRefPotId);
				info.setRoleId(argRoleId);
				roleIdInfos.put(argRoleId, info);
			}
		} else {
			roleIdInfos = Collections.emptyMap();
		}
		return roleIdInfos;
	}
	
	private <T extends ReturnIdInfo> T maybeCollectReturn(String declaredIn,
			IChunk refPot, String returnId, Function<String,T> factory) {
		if(declaredIn != null) {
			return factory.apply(returnId);
		} else {
			return null;
		}
	}
	
	private <T extends RoleIdInfo> void maybeLinkReferentOfCallableInvocationToArgumentsAndUpdateRoleIds(
			IDeclarativeModule dm, String declaredIn, IChunk referencePotential,
			IChunk referent, RequestData data, Map<String,T> roleInfos)
					throws InterruptedException, ExecutionException {
		if(declaredIn != null) {
			for(T translation: roleInfos.values()) {
				if(translation.getArgumentReferencePotential() != null) {
					translation.setArgumentReferent(
							(IChunk)translation.getArgumentReferencePotential()
								.getMetaData(ITWM.referent));
					if(translation.getArgumentReferent() != null
						/* && !translation.getArgumentReferent().getSymbolicChunk().getName().equals("error")*/) {
						try {
							// TODO: it is actually the property of the referent that contains
							// the value and needs to be locked
							if(translation.getRoleContainer() != null) {
								replaceFeature(referent, translation.getRoleContainer(),
									translation.getRole(), translation.getArgumentReferent());
								translation.setRoleId(translation.getArgumentReferent().getSymbolicChunk().getName());
							}
						} catch(Exception e) {
							LOGGER.error("Failed to replace #"+translation.getRoleId()+": "
									+(translation.getRole()==null?null:translation.getRole().getSymbolicChunk().getName())
									+" by "+(translation.getArgumentReferent()==null?null:translation.getArgumentReferent().getSymbolicChunk().getName())
									+" (referred to by "+translation.getArgumentReferencePotential()+")"
									+" in "+referent.getSymbolicChunk().getName()
									+".features="+translation.getRoleContainer()
									+": "+e.getMessage(), e);
						}
					}
				}
				module.fireLinkReferentOfCallableInvocationToArgumentsAndUpdateRoleIds(
						referencePotential, referent, declaredIn, translation.getRoleContainer(),
						translation.getRoleId(),
						translation.getRole(),
						translation.getArgumentReferencePotentialId(),
						translation.getArgumentReferencePotential(),
						translation.getArgumentReferent(),
						data.startTime, data.endTime, data.requestChunk);
			}
		}
		// ... and update the roleId to the name of these referents, thereafter.
		updateRoleIds(dm, referencePotential, data, roleInfos);
	}
	
	/**
	 * Links the argument's referent to the referent of the callable invocation,
	 * if
	 * (a) the reference potential of the callable invocation has been
	 *     passed from REMMA to TWM in the form of a chunk, and
	 * (b) the reference potential has a referent.
	 * 
	 * @param roleIn
	 * @param roleId
	 * @param dm
	 * @param referent
	 * @return RoleIdInfo A role id info that reflects the referent being used as new
	 * 	argument in the callable invocation that should replace the previous argument.
	 * 	Null, if the argument was not linked to a method.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private RoleIdInfo maybeLinkArgumentToReferentOfCallableInvocation(String argumentReferencePotentialId,
			String roleIn, String roleId,
			IDeclarativeModule dm, IChunk referent, RequestData data)
			throws InterruptedException, ExecutionException {
		IChunk callableInvocation = roleIn!=null?dm.getChunk(roleIn).get():null;
		if(callableInvocation != null) {

			// The method invocation has been read by REMMA already
			if(roleId == null)
				throw new IllegalStateException("RoleIn="+roleIn+" lacks roleId");

			IChunk callableInvocationReferent = (IChunk)callableInvocation.getMetaData(ITWM.referent);
			FeatureData toReplace = null;
			if(callableInvocationReferent != null) {

				// The method invocation has been referentialised already
				toReplace = findFeatureByName(callableInvocationReferent, roleId);
				if(toReplace == null) {
					// TODO: There are problems with re-activated definite arguments that
					// refer to callable invocations
				} else {
				
					// Replace the value by the referent
					replaceFeature(toReplace.featureSource, toReplace.features,
							toReplace.feature, referent);
					
					RoleIdTranslation info = new RoleIdTranslation(argumentReferencePotentialId);
					info.setRoleId(roleId); // old roleId
					info.setRoleId(referent.getSymbolicChunk().getName()); // new roleId
					info.setRole(referent);
					info.setRoleContainer(toReplace.features);
					return info;
					
					// TODO: LATER: Shouldn't the parameter name be merged with the
					// argument instead of being replaced by the argument (in case the
					// parameter name is activated above retrieval threshold)
				}
			} // TODO: else
			
			module.fireLinkArgumentToReferentOfCallableInvocation(roleIn,
					callableInvocation, callableInvocationReferent,
					roleId, (toReplace==null?null:toReplace.feature), referent,
					data.startTime, data.endTime, data.requestChunk);
			
		}
		return null;
	}
	
	/**
	 * Writes the (potentially modified) role ids to internal caches and the
	 * reference potential chunks of the argument reference potentials (in case
	 * these chunks exist).
	 * 
	 * @param dm
	 * @param roleIdTranslations
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private <T extends ReturnIdInfo> void updateRoleIds(IDeclarativeModule dm,
			IChunk referencePotential, RequestData data, Map<String,T> roleIdTranslations)
			throws InterruptedException, ExecutionException {
		updateRoleIds(dm, referencePotential, data, roleIdTranslations.values());
	}
	
	private <T extends ReturnIdInfo> void updateRoleIds(IDeclarativeModule dm,
			IChunk referencePotential, RequestData data, Collection<T> translations)
			throws InterruptedException, ExecutionException {	
		for(T trans: translations) {
			if(trans instanceof ReturnIdTranslation) {
				ReturnIdTranslation translation = (ReturnIdTranslation)trans;
				data.returnId = translation.getRoleId();
				doLocked(referencePotential, c -> {
					c.setMetaData(ITWM.returnId, translation.getRoleId());
				});
			} else if(trans instanceof RoleIdInfo) {
				RoleIdInfo roleInfo = (RoleIdInfo)trans;
				
				// The internal data structures of the TWM module need to be updated
				module.referencePotentials.get(roleInfo.getArgumentReferencePotentialId())
					.setRoleId(roleInfo.getRoleId());
				module.roleIds.put(roleInfo.getArgumentReferencePotentialId(), roleInfo.getRoleId());
				
				// There will be a chunk for the argument reference potential, if it has been
				// passed from REMMA to TWM already.
				IChunk argumentRefPotChunk = dm.getChunk(roleInfo.getArgumentReferencePotentialId()).get();
				if(argumentRefPotChunk != null) {
					roleInfo.setArgumentReferencePotential(argumentRefPotChunk);
					TWMModule.doLocked(argumentRefPotChunk, c -> {
						c.setMetaData(ITWM.roleId, roleInfo.getRoleId());
					});
				}
			} else {
				throw new IllegalStateException("Invalid ReturnIdInfo subclass: "+trans.getClass().getName());
			}
		}
	}
	
	private void replaceFeature(IChunk chunkToLock, ICollectionSlot featuresSlot,
			IChunk oldFeature, IChunk newFeature) {
		TWMModule.doLocked(chunkToLock, src -> {
			featuresSlot.remove(oldFeature);
			featuresSlot.add(newFeature);
		});
	}
	
	/**
	 * Returns a feature chunk with the given name. This method will recurse for
	 * technical features, if no feature is found in the given feature source. 
	 * 
	 * @param featureSource
	 * @param featureName
	 * @return
	 * @see ITWM#isTechnical
	 */
	private FeatureData findFeatureByName(IChunk featureSource, String featureName) {
		
		ICollectionSlot featuresSlot = TWMModule.getFeaturesSlot(featureSource);
		List<IChunk> technicalChunks = new LinkedList<IChunk>();
		
		// Find direct feature by name
		for(Object featureObj: featuresSlot.getValues()) {
			FeatureData featureData = ensureFeatureIsAChunk(featureObj, featureSource,
					f -> {
						ISymbolicChunk fsc = f.getSymbolicChunk();
						if(fsc.getName().equals(featureName)) {
							return new FeatureData(featureSource, featuresSlot, f);
						} else {
							if((Boolean)f.getMetaData(ITWM.isTechnical)) {
								technicalChunks.add(f);
							}
						}
						return null;
					});
			if(featureData != null) {
				return featureData;
			}
		}
		
		// Recurse for technical features
		for(IChunk technicalChunk: technicalChunks) {
			FeatureData featureData = findFeatureByName(technicalChunk, featureName);
			if(featureData != null)
				return featureData;
		}
		
		// No matching feature found
		return null;
	}
	
	protected class RequestData extends WordData {
		
		private final List<Function<IScopedRetrievalModule,Double>> completionDurationSuppliers = new ArrayList<>(); 
		private final List<Consumer<RequestData>> finishActions = new ArrayList<>();
		private IChunk referent;
		private String coReferenceChain, declaredIn, roleIn, roleId, returnId;
		
		private RequestData(IChunk requestChunk) {
			super(requestChunk);
		}
		
	}
	
	protected class FeatureData {
		
		private final IChunk featureSource;
		private final ICollectionSlot features;
		private final IChunk feature;
		
		public FeatureData(IChunk featureSource, ICollectionSlot features,
				IChunk feature) {
			this.featureSource = featureSource;
			this.features = features;
			this.feature = feature;
		}
		
	}
	
	protected class ReturnIdInfo {
		
		private ICollectionSlot roleContainer;
		private String roleId;
		private IChunk role;
		
		public ReturnIdInfo() {
		}
		
		public ReturnIdInfo(String roleId) {
			this.roleId = roleId;
		}
		
		protected ICollectionSlot getRoleContainer() {
			return roleContainer;
		}
		
		protected void setRoleContainer(ICollectionSlot roleContainer) {
			this.roleContainer = roleContainer;
		}
		
		protected String getRoleId() {
			return roleId;
		}
		
		protected void setRoleId(String roleId) {
			this.roleId = roleId;
		}
		
		protected IChunk getRole() {
			return role;
		}
		
		protected void setRole(IChunk role) {
			this.role = role;
		}
		
	}
	
	protected class RoleIdInfo extends ReturnIdInfo {
		
		private final String argumentReferencePotentialId;
		private IChunk argumentReferencePotential;
		private IChunk argumentReferent;

		public RoleIdInfo(String argumentReferencePotentialId) {
			this.argumentReferencePotentialId = argumentReferencePotentialId;
		}
		
		protected String getArgumentReferencePotentialId() {
			return argumentReferencePotentialId;
		}
		
		protected IChunk getArgumentReferencePotential() {
			return argumentReferencePotential;
		}
		
		protected void setArgumentReferencePotential(IChunk argumentReferencePotential) {
			this.argumentReferencePotential = argumentReferencePotential;
		}
		
		protected IChunk getArgumentReferent() {
			return argumentReferent;
		}
		
		protected void setArgumentReferent(IChunk argumentReferent) {
			this.argumentReferent = argumentReferent;
		}

	}
	
	protected class ReturnIdTranslation extends ReturnIdInfo {
		
		private String oldRoleId;
		
		private ReturnIdTranslation(String roleId) {
			super(roleId);
		}
		
		protected void setRoleId(String roleId) {
			oldRoleId = getRoleId();
			super.setRoleId(roleId);
		}
		
		protected String getOldRoleId() {
			return oldRoleId;
		}
		
	}
	
	protected class RoleIdTranslation extends RoleIdInfo {
		
		private String oldRoleId;
		
		private RoleIdTranslation(String argumentReferencePotentialId) {
			super(argumentReferencePotentialId);
		}
		
		protected void setRoleId(String roleId) {
			oldRoleId = getRoleId();
			super.setRoleId(roleId);
		}
		
		protected String getOldRoleId() {
			return oldRoleId;
		}
	}
	
	@FunctionalInterface
	public interface FeaturePassing {
		public void pass(IChunk feature, IChunk featureCopy, ICollectionSlot featuresSlot);
	}
}
