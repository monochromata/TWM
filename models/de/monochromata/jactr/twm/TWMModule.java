package de.monochromata.jactr.twm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;










import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonreality.agents.IAgent;
import org.commonreality.object.manager.event.IAfferentListener;
import org.commonreality.object.manager.event.IEfferentListener;
import org.jactr.core.buffer.IActivationBuffer;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunk.ISymbolicChunk;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.concurrent.ExecutorServices;
import org.jactr.core.model.IModel;
import org.jactr.core.model.event.IModelListener;
import org.jactr.core.model.event.ModelEvent;
import org.jactr.core.model.event.ModelListenerAdaptor;
import org.jactr.core.module.declarative.IDeclarativeModule;
import org.jactr.core.runtime.ACTRRuntime;
import org.jactr.core.slot.IMutableSlot;
import org.jactr.core.slot.ISlot;
import org.jactr.modules.pm.AbstractPerceptualModule;










import de.monochromata.jactr.dm.INonMergingDeclarativeModule;
import de.monochromata.jactr.tls.AnaphorInfo;
import de.monochromata.jactr.tls.ConceptualSchema;
import de.monochromata.jactr.tls.ICollectionSlot;
import de.monochromata.jactr.tls.LexicalFeatures;
import de.monochromata.jactr.tls.LexicalizedConceptualSchema;
import de.monochromata.jactr.tls.ReferencePotential;
import de.monochromata.jactr.tls.Schema;
import de.monochromata.jactr.tls.Scope;
import de.monochromata.jactr.tls.Word;
import static de.monochromata.jactr.twm.TWMParticipant.*;

/**
 * IModule is the entry point to extend an ACT-R model from a theoretical point.
 * Usually, modules are instantiated, have their parameters set and are then
 * attached to the IModel via IModel.install(IModule), which in turn calls
 * IModule.install(IModel). <br>
 * <br>
 * Most behavior is extended by attaching listeners to the model and its
 * contents. Care must be taken when doing this because of the threaded nature
 * of models.<br>
 * <br>
 */
public class TWMModule extends AbstractPerceptualModule implements ITWM {

	/**
	 * Standard logger used through out jACT-R
	 */

	private static final transient Log LOGGER = LogFactory
			.getLog(TWMModule.class);

	static public final String MODULE_NAME = "TWM";

	/**
	 * if you need to be notified when an IAfferentObject is added, modified, or
	 * removed you've instantiate one of these and attach it down in #install
	 */
	private IAfferentListener _afferentListener;

	/**
	 * if you need to be notified when an IEfferentObject is added, removed or
	 * modified you'd instantiate one of these and attach it down in #install
	 */
	private IEfferentListener _efferentListener;
	
	private List<ITWMListener> listeners = new ArrayList<>();
	
	private final Map<String,String> parameterMap = TWMParticipant.createParameterMap();

	private IChunkType conceptualSchemaChunkType,
			lexicalizedConceptualSchemaChunkType,
			wordChunkType,
			referencePotentialChunkType;
	private double schemaCreationDuration = Double.NaN;
	
	private TWMBuffer twmBuffer;
	private TWMGraphemicBuffer twmGraphemicBuffer;
	
	/** 1:n: String:coReferenceChainId -> String:referencePotentialId */
	final Map<String,List<String>> coReferenceChains = new HashMap<>();
	/** 1:1: String:coReferenceChainId -> IChunk:referent */
	final Map<String,IChunk> coReferenceChainReferents = new HashMap<>();
	
	List<ConceptualSchema> conceputalSchemata = new LinkedList<>();
	/** 1:1: String:schemaId -> ConceptualSchema:schema */
	Map<String,ConceptualSchema> conceputalSchemataByName = new HashMap<>();
	
	/**
	 * Maps a schema ID to all schemata that are (transitively)
	 * contained in it as features and do not have a corresponding
	 * reference potential. Because dependent features do not have
	 * a reference potential (because they have no spatial information),
	 * they cannot be encoded (added to declarative memory) when they
	 * are read and need to be encoded when another schema is encoded that
	 * contains them (transitively) as a feature.
	 * 
	 * 1:n: String:schemaId -> String:schemaId
	 */
	final Map<String,List<String>> dependentSchemata = new HashMap<>();
	
	/**
	 * Maps IDs of features that have not been encoded (added to declarative memory)
	 * to the chunks of schemata that need to refer to the feature after it has been
	 * instantiated.
	 * 
	 * 1:n: String:featureId -> IChunk:schema
	 */
	final Map<String,List<IChunk>> missingReferences = new HashMap<>();
	
	/** 1:1: String:referencePotentialId -> ReferencePotential */
	final Map<String,ReferencePotential> referencePotentials = new HashMap<>();
	
	/** String:schemaId - all schemata that are targeted by a declaredIn or schema attribute */
	final Set<String> referredToByReferencePotential = new HashSet<>();
	
	/** 1:n: String:roleIn:referencePotentialId -> String:argument:referencePotentialId */
	final Map<String,List<String>> arguments = new HashMap<>();
	/** 1:n: String:argument:referencePotentialId -> String:roleId:tokenId */
	final Map<String,String> roleIds = new HashMap<>();
	
	/**
	 * standard 0 argument constructor must always be present. this should do
	 * very little.
	 */
	public TWMModule() {
		super(MODULE_NAME);
	}

	/**
	 * @see org.jactr.core.module.AbstractModule#install(org.jactr.core.model.IModel)
	 */
	@Override
	public void install(IModel model) {
		super.install(model);

		IModelListener startUp = new ModelListenerAdaptor() {

			/**
			 * called once the connection to common reality is established. Once
			 * this occurs, we can get access to the common reality executor
			 * 
			 * @see org.jactr.core.model.event.ModelListenerAdaptor#modelConnected(org.jactr.core.model.event.ModelEvent)
			 */
			public void modelConnected(ModelEvent event) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Connected to common reality, attaching listeners");

				/*
				 * all AbstractPerceptualModules within a single model share a
				 * common executor (on a separate thread) that is to be used to
				 * process events coming from Common Reality. This executor is
				 * only available after modelConnected()
				 *
				Executor executor = getCommonRealityExecutor();

				/*
				 * the agent interface is how we communicate with common reality
				 *
				IAgent agentInterface = ACTRRuntime.getRuntime().getConnector()
						.getAgent(event.getSource());

				/*
				 * now, whenever an event comes from common reality to the agent
				 * interface, we will receive notification of the changes on the
				 * common reality executor thread.
				 */
				// agentInterface.addListener(_afferentListener, executor);
				// agentInterface.addListener(_efferentListener, executor);
			}
		};

		/*
		 * we attach this listener with the inline executor - i.e. it will be
		 * called on the same thread that issued the event (the ModelThread),
		 * immediately after it occurs.
		 */
		model.addListener(startUp, ExecutorServices.INLINE_EXECUTOR);
	}

	/**
	 * if you want to install some buffers, replace this code
	 */
	protected Collection<IActivationBuffer> createBuffers() {
		twmGraphemicBuffer = new TWMGraphemicBuffer("twmGraphemic", this,
				Integer.parseInt(getParameter(TWM_GRAPHEMIC_BUFFER_CAPACITY)));
		twmBuffer = new TWMBuffer("twm", this,
				Integer.parseInt(getParameter(TWM_BUFFER_CAPACITY)),
				twmGraphemicBuffer);
		return Arrays.asList(new IActivationBuffer[] { twmGraphemicBuffer, twmBuffer });
	}

	/**
	 * called after all the chunktypes, chunks, and productions have been
	 * installed, but before any instruments or extensions have been installed.
	 * If you need to attach to any other modules it should be done here.
	 * However, if you need to know about production or chunk creation events,
	 * you should attach listenes during install(IModel)
	 */
	@Override
	public void initialize() {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("initializing " + getClass().getSimpleName());
	}

	
	
	@Override
	public Collection<String> getSetableParameters() {
		return getPossibleParameters();
	}

	@Override
	public Collection<String> getPossibleParameters() {
		return parameterMap.keySet();
	}

	@Override
	public void setParameter(String key, String value) {
		if(parameterMap.containsKey(key)) {
			parameterMap.put(key, value);
		} else {
			super.setParameter(key, value);
		}
	}

	@Override
	public String getParameter(String key) {
		if(parameterMap.containsKey(key)) {
			return parameterMap.get(key);
		} else {
			return super.getParameter(key);
		}
	}

	@Override
	public void reset() {
		coReferenceChains.clear();
		coReferenceChainReferents.clear();
		referencePotentials.clear();
		referredToByReferencePotential.clear();
		arguments.clear();
		roleIds.clear();
	}

	/**
	 * please, for the love of god, dispose of your resources
	 * 
	 * @see org.jactr.core.module.AbstractModule#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
	}
	
	
	
	IChunkType getConceptualSchemaChunkType() throws InterruptedException, ExecutionException {
		return getSchemaChunkType(
				() -> { return conceptualSchemaChunkType; },
				ct -> { conceptualSchemaChunkType = ct; },
				CONCEPUTAL_SCHEMA_CHUNK_TYPE);
	}
	
	IChunkType getLexicalizedConceptualSchemaChunkType() throws InterruptedException, ExecutionException {
		return getSchemaChunkType(
				() -> { return lexicalizedConceptualSchemaChunkType; },
				ct -> { lexicalizedConceptualSchemaChunkType = ct; },
				LEXICALIZED_CONCEPUTAL_SCHEMA_CHUNK_TYPE);
	}
	
	IChunkType getWordChunkType() throws InterruptedException, ExecutionException {
		return getSchemaChunkType(
				() -> { return wordChunkType; },
				ct -> { wordChunkType = ct; },
				WORD_CHUNK_TYPE);
	}
	
	IChunkType getReferencePotentialChunkType() throws InterruptedException, ExecutionException {
		return getSchemaChunkType(
				() -> { return referencePotentialChunkType; },
				ct -> { referencePotentialChunkType = ct; },
				REFERENCE_POTENTIAL_CHUNK_TYPE);
	}
	
	private IChunkType getSchemaChunkType(Supplier<IChunkType> supplier, Consumer<IChunkType> consumer,
			String parameterName) throws InterruptedException, ExecutionException {
		if(supplier.get() == null) {
			String chunkTypeName = getParameter(parameterName);
			CompletableFuture<IChunkType> future = getModel().getDeclarativeModule().getChunkType(chunkTypeName);
			IChunkType t = future.get();
			consumer.accept(t);
		}
		return supplier.get();
	}
	
	public double getSchemaCreationDuration() {
		if(Double.isNaN(schemaCreationDuration))
			schemaCreationDuration = Double.parseDouble(getParameter(SCHEMA_CREATION_DURATION_S));
		return schemaCreationDuration;
	}
	
	private IChunk createSchema(IChunkType chunkType, String id, List<String> featureChunkNames,
			BiConsumer<IChunk,ISymbolicChunk> customizer, boolean isTechnical, boolean isToken,
			boolean isObject, Scope scope) throws InterruptedException, ExecutionException {
		
		// Make sure a non-merging declarative module is used
		INonMergingDeclarativeModule dm = (INonMergingDeclarativeModule)
				getModel().getDeclarativeModule();
		
		// Create chunk
		IChunk chunk = dm.createChunk(chunkType, id).get();
		
		// Log an error if the designated chunk name had already been taken
		String chunkName = chunk.getSymbolicChunk().getName();
		if(!chunkName.equals(id)) {
			IChunk existingChunk = dm.getChunk(id).get();
			LOGGER.warn("The id="+id+" could not be used for a new schema, because"
					+" "+existingChunk+" uses that name already. The id="+chunkName
					+" is used instead.");
		}
		
		// Add meta-data
		chunk.setMetaData(ITWM.isTechnical, isTechnical);
		chunk.setMetaData(ITWM.scope, scope);
		
		// Add slots
		ISymbolicChunk sc = chunk.getSymbolicChunk();
		((IMutableSlot)sc.getSlot("typeVsToken")).setValue(isToken?"token":"type");
		((IMutableSlot)sc.getSlot("objectVsMethod")).setValue(isObject?"object":"method");
		Collection<Object> featureChunks = new ArrayList<Object>(featureChunkNames.size());
		for(String featureName: featureChunkNames) {
			IChunk featureChunk = dm.getChunk(featureName).get();
			if(featureChunk == null) {
				addMissingReference(featureName, chunk);
			} else {
				featureChunks.add(featureChunk);
			}
		}
		((ICollectionSlot)sc.getSlot("features")).addAll(featureChunks);
		customizer.accept(chunk, sc);
		
		// Resolve references to this schema that are marked
		// as missing in other chunks by adding them to these chunks
		List<IChunk> references = missingReferences.remove(id);
		if(references != null) {
			for(IChunk reference: references) {
				ICollectionSlot features = getFeaturesSlot(reference);
				doLocked(reference, c -> {
					features.add(chunk);
				});
			}
		}
		
		// TODO: What about activation? Add initial references? Or fit them?
		
		return chunk;
	}
	
	private void addMissingReference(String featureName, IChunk schema) {
		List<IChunk> references = missingReferences.get(featureName);
		if(references == null) {
			references = new ArrayList<IChunk>();
			missingReferences.put(featureName, references);
		}
		references.add(schema);
	}
	
	@Override
	public IChunk createConceptualSchema(String id, List<String> features,
			boolean isTechnical, boolean isToken, boolean isObject, Scope scope)
			throws InterruptedException, ExecutionException {
		return createSchema(getConceptualSchemaChunkType(), id, features, (chunk, sc) -> {},
				isTechnical, isToken, isObject, scope);
	}

	@Override
	public IChunk createLexicalizedConceptualSchema(String id, String graphemic,
			List<String> featureChunkNames, boolean isTechnical, boolean isToken,
			boolean isObject, Scope scope) throws InterruptedException, ExecutionException {
		return createSchema(getLexicalizedConceptualSchemaChunkType(), id, featureChunkNames,
			(chunk, sc) -> { ((IMutableSlot)sc.getSlot("graphemic")).setValue(graphemic); },
			isTechnical, isToken, isObject, scope);

	}

	@Override
	public IChunk toChunk(Word word) throws InterruptedException,
			ExecutionException {
		if(word instanceof ReferencePotential) {
			return toChunk((ReferencePotential)word);
		} else {
			return createWordChunk(word, getWordChunkType());
		}
	}
	
	@Override
	public void setAnaphorMetaData(IChunk chunk, Word word) {
		AnaphorInfo anaphorInfo = word.getAnaphorInfo();
		if(anaphorInfo != null) {
			chunk.setMetaData(ITWM.anaphorInfo, anaphorInfo);
		}
	}
	
	private IChunk createWordChunk(Word word, IChunkType chunkType)
			throws InterruptedException, ExecutionException {
		// Create chunk
		IDeclarativeModule dm = getModel().getDeclarativeModule();
		IChunk chunk = dm.createChunk(chunkType, word.getId()).get();
		
		// Add meta-data
		chunk.setMetaData(ITWM.scope, word.getScope());
		chunk.setMetaData(ITWM.uri, word.getSpatial().getUri());
		chunk.setMetaData(ITWM.line, word.getSpatial().getLine());
		chunk.setMetaData(ITWM.column, word.getSpatial().getColumn());
		chunk.setMetaData(ITWM.length, word.getSpatial().getLength());
		
		// Add anaphor meta-data
		setAnaphorMetaData(chunk, word);
		
		// Add graphemic
		((IMutableSlot)chunk.getSymbolicChunk().getSlot("graphemic")).setValue(word.getGraphemic());
		
		return chunk;
	}

	@Override
	public IChunk toChunk(ReferencePotential refPot)
			throws InterruptedException, ExecutionException {
		IChunk chunk = createWordChunk(refPot, getReferencePotentialChunkType());
		
		// Add additional meta-data
		chunk.setMetaData(ITWM.referent, refPot.getReferent());
		chunk.setMetaData(ITWM.coReferenceChain, refPot.getCoReferenceChain());
		chunk.setMetaData(ITWM.declaredIn, refPot.getDeclaredIn());
		chunk.setMetaData(ITWM.schema, refPot.getSchema());
		chunk.setMetaData(ITWM.roleIn, refPot.getRoleIn());
		chunk.setMetaData(ITWM.roleId, refPot.getRoleId());
		chunk.setMetaData(ITWM.returnId, refPot.getReturnId());
		
		// Add additional slot: isDefinite
		((IMutableSlot)chunk.getSymbolicChunk().getSlot("isDefinite")).setValue(refPot.isDefinite());
		
		return chunk;
	}

	@Override
	public IChunk toChunk(ConceptualSchema schema) throws InterruptedException, ExecutionException {
		if(schema instanceof LexicalizedConceptualSchema) {
			return toChunk((LexicalizedConceptualSchema)schema);
		} else {
			return createConceptualSchema(schema.getId(),
					schema.getConceptualFeatures(), schema.isTechnical(),
					getSchemaTokenVsTypeBoolean(schema),
					schema.isObject(), schema.getScope());
		}
	}

	@Override
	public IChunk toChunk(LexicalizedConceptualSchema schema) throws InterruptedException, ExecutionException {
		return createLexicalizedConceptualSchema(schema.getId(),
				(String)schema.getLexicalFeatures().get(LexicalFeatures.graphemic.name()),
				schema.getConceptualFeatures(), schema.isTechnical(),
				getSchemaTokenVsTypeBoolean(schema),
				schema.isObject(), schema.getScope());
	}
	
	private boolean getSchemaTokenVsTypeBoolean(Schema schema) {
		String schemaType = schema.getSchemaType();
		if(schemaType.equals(Schema.TOKEN)) {
			return true;
		} else if(schemaType.equals(Schema.TYPE)) {
			return false;
		} else {
			throw new IllegalArgumentException("Invalid schema type: "+schemaType);
		}
	}
	
	@Override
	public void addToCoReferenceChain(String referencePotentialId, String coReferenceChain) {
		addToMapOfLists(coReferenceChain, referencePotentialId,
				() -> { return coReferenceChains; });
	}
	
	@Override
	public void setConceptualSchemata(List<ConceptualSchema> schemata,
			Map<String, ConceptualSchema> schemataByName) {
		
		this.conceputalSchemata = schemata;
		this.conceputalSchemataByName = schemataByName;
		
		// Make sure a non-merging declarative module is used
		INonMergingDeclarativeModule dm = (INonMergingDeclarativeModule)
				getModel().getDeclarativeModule();
		
		// Create dependent schemata for chunks that are not yet encoded
		for(ConceptualSchema schema: schemata) {
			try {
				if(dm.getChunk(schema.getId()).get() == null) {
					List<String> dependentFeatures =
							collectDependentSchemata(schemataByName, schema);
					if(dependentFeatures != null)
						dependentSchemata.put(schema.getId(), dependentFeatures);
				}
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error("Failed to check if schema "+schema.getId()
						+" is in declarative memory already: "+e.getMessage(), e);
			}
		}
	}

	/**
	 * All features that do not have a reference potential on their own
	 * depend on all those conceptual schemata that are available via
	 * reference potentials.
	 * 
	 * @param schemata
	 * @param schema
	 * @return
	 */
	private List<String> collectDependentSchemata(
			Map<String,ConceptualSchema> schemata, ConceptualSchema schema) {
		List<String> dependentFeatures = null;
		for(String featureId: schema.getConceptualFeatures()) {
			ConceptualSchema feature = schemata.get(featureId);
			if(feature == null) {
				throw new IllegalStateException("Could not find feature "
						+featureId+" referenced in "+schema);
			} else if(!thereIsAReferencePotentialForSchema(featureId)) {
				if(dependentFeatures == null)
					dependentFeatures = new ArrayList<String>();
				dependentFeatures.add(featureId);
				List<String> childDependencies =
						collectDependentSchemata(schemata, feature);
				if(childDependencies != null)
					dependentFeatures.addAll(childDependencies);
			}
		}
		return dependentFeatures;
	}
	
	private boolean thereIsAReferencePotentialForSchema(String schemaId) {
		return referredToByReferencePotential.contains(schemaId);
	}
	
	
	
	@Override
	public boolean isDependentSchema(String schemaId) {
		return dependentSchemata.containsValue(schemaId);
	}

	@Override
	public IChunk createConceptualSchemaAndDependentSchemataAndAddThemToDM(
			String schemaId) {
		IChunk schemaChunk = null;
		try {
			// Make sure a non-merging declarative module is used
			INonMergingDeclarativeModule dm = (INonMergingDeclarativeModule)
					getModel().getDeclarativeModule();
			
			// Instantiate named schemata
			ConceptualSchema schema = conceputalSchemataByName.get(schemaId);
			schemaChunk = toChunk(schema);
			addToDMAndEnsureNameIsUnique(dm, schemaChunk);
			
			// Instantiate dependent schemata that do not exist yet
			List<String> dependees = dependentSchemata.remove(schemaId);
			if(dependees != null) {
				for(String dependee: dependees) {
					ConceptualSchema dependeeSchema = conceputalSchemataByName.get(dependee);
					IChunk dependeeChunk = dm.getChunk(dependeeSchema.getId()).get();
					if(dependeeChunk == null) {
						// Recurse for missing dependees
						createConceptualSchemaAndDependentSchemataAndAddThemToDM(dependee);
					}
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error("Failed to schema "+schemaId
					+" and dependent schemata: "+e.getMessage(), e);
		}
		return schemaChunk;
	}

	@Override
	public void addToDMAndEnsureNameIsUnique(IDeclarativeModule dm, IChunk chunk) {
		try {
			IChunk addedChunk = dm.addChunk(chunk).get();
			String givenName = chunk.getSymbolicChunk().getName();
			String addedName = addedChunk.getSymbolicChunk().getName();
			if(!addedName.equals(givenName)) {
				throw new IllegalStateException("Added chunk="
						+addedChunk+" is not unique (given="+chunk+")");
			}
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error("Failed to add to dm: "+chunk+": "+e.getMessage(), e);
		}
	}
	
	@Override
	public boolean isReferredToByReferencePotentials(String schemaId) {
		return referredToByReferencePotential.contains(schemaId);
	}
	
	@Override
	public void addReferencePotential(ReferencePotential refPot) {
		referencePotentials.put(refPot.getId(), refPot);
		if(refPot.getDeclaredIn() != null) {
			referredToByReferencePotential.add(refPot.getDeclaredIn());
		} else if(refPot.getSchema() != null) {
			referredToByReferencePotential.add(refPot.getSchema());
		}
	}

	@Override
	public ReferencePotential getReferencePotential(String id) {
		return referencePotentials.get(id);
	}

	@Override
	public boolean hasReferencePotential(String id) {
		return referencePotentials.containsKey(id);
	}

	@Override
	public Set<String> getReferencePotentialIds() {
		return Collections.unmodifiableSet(referencePotentials.keySet());
	}

	@Override
	public Collection<ReferencePotential> getReferencePotentials() {
		return Collections.unmodifiableCollection(referencePotentials.values());
	}

	@Override
	public void prepareArguments(String invocationReferencePotentialId) {
		initMapOfLists(invocationReferencePotentialId, () -> {
			return arguments;
		});
	}
	
	@Override
	public void addArgument(String invocationReferencePotentialId,
			String argumentReferencePotentialId, String roleId) {
		addToMapOfLists(invocationReferencePotentialId, argumentReferencePotentialId,
				() -> { return arguments; });
		roleIds.put(argumentReferencePotentialId, roleId);
	}
	
	protected void addToMapOfLists(String key, String listElement,
			Supplier<Map<String,List<String>>> map) {
		List<String> elements = initMapOfLists(key, map);
		elements.add(listElement);
	}
	
	protected List<String> initMapOfLists(String key,
			Supplier<Map<String,List<String>>> map) {
		List<String> elements = map.get().get(key);
		if(elements == null) {
			elements = new LinkedList<String>();
			map.get().put(key, elements);
		}
		return elements;
	}

	static ICollectionSlot getFeaturesSlot(IChunk chunk) {
		ISlot slot = chunk.getSymbolicChunk().getSlot("features");
		if(slot == null) {
			throw new IllegalArgumentException("Has no features slot: "+chunk);
		} else if(!(slot instanceof ICollectionSlot)) {
			throw new IllegalStateException("Has no collection slot for features: "+chunk);
		} else {
			return (ICollectionSlot)slot;
		}
	}
	
	static void doLocked(IChunk chunk, Consumer<IChunk> lockedAction) {
		try {
			chunk.getWriteLock().lock();
			lockedAction.accept(chunk);
		} finally {
			chunk.getWriteLock().unlock();
		}
	}

	@Override
	public void addListener(ITWMListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ITWMListener listener) {
		listeners.remove(listener);
	}
	
	protected void fireRequestWordActivation(double requestTime, IChunk wordChunk) {
		listeners.forEach( l -> {
			l.requestWordActivation(requestTime, wordChunk);
		});
	}
	
	protected void fireRequestReferentialisation(double requestTime, IChunk referencePotentialChunk) {
		listeners.forEach( l -> {
			l.requestReferentialisation(requestTime, referencePotentialChunk);
		});
	}
	
	protected void fireSpecify(IChunk featureReceiver, IChunk featureProvider,
			double startTime, double endTime, IChunk requestChunk) {
		listeners.forEach( l -> {
			l.specify(featureReceiver, featureProvider,
				startTime, endTime, requestChunk);
		});
	}
	
	protected void fireInstantiate(IChunk typeSchema, double startTime,
			double endTime, IChunk requestChunk) {
		listeners.forEach( l -> {
			l.instantiate(typeSchema, startTime, endTime, requestChunk);
		});
	}
	
	protected void fireLinkArgumentToReferentOfCallableInvocation(String roleIn, IChunk callableInvocation,
			IChunk callableInvocationReferent, String roleId, IChunk oldArgument, IChunk newArgument,
			double startTime, double endTime, IChunk requestChunk) {
		listeners.forEach( l -> {
			l.linkArgumentToReferentOfCallableInvocation(roleIn, callableInvocation,
					callableInvocationReferent, roleId, oldArgument, newArgument,
					startTime, endTime, requestChunk);
		});
	}
	
	protected void fireLinkReferentOfCallableInvocationToArgumentsAndUpdateRoleIds(
			IChunk referencePotential, IChunk referent, String declaredIn,
			ISlot roleContainer, String roleId, IChunk role, String argumentReferencePotentialId,
			IChunk argumentReferencePotential, IChunk argumentReferent,
			double startTime, double endTime, IChunk requestChunk) {
		listeners.forEach( l -> {
			l.linkReferentOfCallableInvocationToArgumentsAndUpdateRoleIds(
					referencePotential, referent, declaredIn,
					roleContainer, roleId, role,
					argumentReferencePotentialId, argumentReferencePotential,
					argumentReferent, startTime, endTime, requestChunk);
		});
	}
	
	protected void fireAccess(String kindOfChunk, IChunk chunk,
			double startTime, double endTime, IChunk requestChunk) {
		listeners.forEach( l -> {
			l.access(kindOfChunk, chunk, startTime, endTime, requestChunk);
		});
	}
}
