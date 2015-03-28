package de.monochromata.jactr.twm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.jactr.core.chunk.IChunk;
import org.jactr.core.module.IModule;
import org.jactr.core.module.declarative.IDeclarativeModule;
import org.jactr.core.slot.ISlot;

import de.monochromata.jactr.tls.ConceptualSchema;
import de.monochromata.jactr.tls.ICollectionSlot;
import de.monochromata.jactr.tls.LexicalizedConceptualSchema;
import de.monochromata.jactr.tls.ReferencePotential;
import de.monochromata.jactr.tls.Scope;
import de.monochromata.jactr.tls.Word;

public interface ITWM extends IModule {
	
	/**
	 * Meta-data key to identify technical chunks that are treated specially during
	 * activation computation. The corresponding value is a Boolean object.
	 * 
	 * @see IChunk#setMetaData(String, Object)
	 */
	public static final String isTechnical = "de.monochromata.jactr.twm.isTechnical";
	
	/**
	 * Meta-data key to identify the scope to which a schema belongs.
	 * 
	 * @see IChunk#setMetaData(String, Object)
	 */
	public static final String scope = "de.monochromata.jactr.twm.scope";
	
	public static final String uri = "de.monochromata.jact.twm.uri";
	public static final String line = "de.monochromata.jactr.twm.line";
	public static final String column = "de.monochromata.jactr.twm.column";
	public static final String length = "de.monochromata.jactr.twm.length";
	
	public static final String referent = "de.monochromata.jactr.twm.referent";
	public static final String coReferenceChain = "de.monochromata.jactr.twm.coReferenceChain";
	public static final String declaredIn = "de.monochromata.jactr.twm.declaredIn";
	public static final String schema = "de.monochromata.jactr.twm.schema";
	public static final String roleIn = "de.monochromata.jactr.twm.roleIn";
	public static final String roleId = "de.monochromata.jactr.twm.roleId";
	public static final String returnId = "de.monochromata.jactr.twm.returnId";
	
	public static final String anaphorInfo = "de.monochromata.jactr.twm.anaphorInfo";
	
	/**
	 * Creates a new chunk from the given word. If the given word is an instance of
	 * {@link ReferencePotential}, {@link #toChunk(ReferencePotential)} is invoked.
	 * 
	 * @param word
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public IChunk toChunk(Word word) throws InterruptedException, ExecutionException;
	
	public void setAnaphorMetaData(IChunk chunk, Word word);
	
	/**
	 * Creates a new chunk for the given reference potential.
	 * 
	 * @param refPot
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public IChunk toChunk(ReferencePotential refPot) throws InterruptedException, ExecutionException;
	
	/**
	 * Creates a chunk for the given schema. The chunk is not added to declarative memory!
	 * 
	 * This method behaves like {@link #toChunk(LexicalizedConceptualSchema)}, if
	 * the given schema is a {@link LexicalizedConceptualSchema}. 
	 * 
	 * @param schema
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @see IDeclarativeModule
	 * @see IDeclarativeModule#addChunk(IChunk)
	 */
	public IChunk toChunk(ConceptualSchema schema) throws InterruptedException, ExecutionException;
	
	/**
	 * Creates a chunk for the given lexicalized schema. The chunk is not added to declarative memory!
	 * 
	 * @param schema
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @see IDeclarativeModule
	 * @see IDeclarativeModule#addChunk(IChunk)
	 */
	public IChunk toChunk(LexicalizedConceptualSchema schema) throws InterruptedException, ExecutionException;
	
	/**
	 * Create a new chunk to represent the given conceptual schema (that is not lexicalized)
	 * and add it to declarative memory.
	 * 
	 * @param id
	 * @param graphemic
	 * @param features
	 * @param isTechnical A meta-data entry that marks technical chunks that
	 * 	are treated specially during activations
	 * @param isToken True, if the schema chunk represents a token schema, false,
	 * 	if it represents a type schema. 
	 * @param isObject True, if the schema chunk represents an object schema, false,
	 * 	if it represents a method schema.
	 * @return
	 * @see #createLexicalizedConceptualSchema(String, List, boolean, boolean, boolean, Scope)
	 */
	public IChunk createConceptualSchema(String id, List<String> featureChunkNames,
			boolean isTechnical, boolean isToken, boolean isObject, Scope scope)
			throws InterruptedException, ExecutionException;
	
	/**
	 * Create a new chunk to represent the given lexicalized conceptual schema
	 * and add it to declarative memory.
	 * 
	 * @param id
	 * @param graphemic
	 * @param features
	 * @param isTechnical A meta-data entry that marks technical chunks that
	 * 	are treated specially during activations
	 * @param isToken True, if the schema chunk represents a token schema, false,
	 * 	if it represents a type schema. 
	 * @param isObject True, if the schema chunk represents an object schema, false,
	 * 	if it represents a method schema.
	 * @return
	 * @see #createConceptualSchema(String, List, boolean, boolean, boolean, Scope)
	 */
	public IChunk createLexicalizedConceptualSchema(String id, String graphemic,
			List<String> featureChunkNames,
			boolean isTechnical, boolean isToken, boolean isObject, Scope scope)
			throws InterruptedException, ExecutionException;
	
	/**
	 * 
	 * 
	 * 
	 * @param schemaId
	 * @return The chunk representation of the schema identified by schemaId
	 */
	public IChunk createConceptualSchemaAndDependentSchemataAndAddThemToDM(String schemaId);
	
	public void setConceptualSchemata(List<ConceptualSchema> schemata, Map<String,ConceptualSchema> schemataByName);
	
	/**
	 * @param schemaId
	 * @return True, if the schema with the given ID is dependent, i.e. has no
	 * 	reference potential and will be added to declarative memory, when another
	 *  schema that is related to it, is added to declarative memory.
	 */
	public boolean isDependentSchema(String schemaId);
	
	public void addToCoReferenceChain(String referencePotentialId, String coReferenceChainId);
	
	public boolean isReferredToByReferencePotentials(String schemaId);
	public void addReferencePotential(ReferencePotential refPot);
	
	public boolean hasReferencePotential(String id);
	public ReferencePotential getReferencePotential(String id);
	public Set<String> getReferencePotentialIds();
	public Collection<ReferencePotential> getReferencePotentials();
	
	/**
	 * Creates an empty list of arguments for the given reference potential
	 * that represents the invocation of a callable. This method is required
	 * because currently e.g. string literals do not count as arguments, but
	 * the declaredIn attribute of the invocation of the callable will trigger
	 * the lookup of potential arguments which would fail if no empty index
	 * of arguments were created.
	 * 
	 * @param invocationReferencePotentialId
	 */
	public void prepareArguments(String invocationReferencePotentialId);
	
	public void addArgument(String invocationReferencePotentialId,
			String argumentReferencePotentialId, String roleId);
	
	/**
	 * Add the given chunk to declarative memory and throw an exception
	 * if the name of the chunk returned after adding differs from the
	 * name of the given chunk.
	 * 
	 * @param dm
	 * @param chunk
	 * @see IDeclarativeModule#addChunk(IChunk)
	 */
	public void addToDMAndEnsureNameIsUnique(IDeclarativeModule dm, IChunk chunk);
	
	public void addListener(ITWMListener listener);
	public void removeListener(ITWMListener listener);

	public static String getObjectVsMethod(IChunk chunk) {
		ISlot slot = chunk.getSymbolicChunk().getSlot("objectVsMethod");
		if(slot instanceof ICollectionSlot) {
			Collection<Object> values = ((ICollectionSlot)slot).getValues();
			if(values.size() != 1) {
				throw new IllegalStateException("More than one value for"
						+" objectVsMethod="+values+" in chunk="+chunk);
			}
			return (String)values.iterator().next();
		} else {
			return (String)slot.getValue();
		}
	}
}
