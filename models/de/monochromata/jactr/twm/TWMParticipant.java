package de.monochromata.jactr.twm;

import java.util.Map;
import java.util.TreeMap;

import org.jactr.io.participant.impl.BasicASTParticipant;

/**
 * The IASTParticipant is responsible for providing IASTInjector and
 * IASTTrimmers, which modify the abstract syntax trees describing models. This
 * participant takes the location of a model descriptor (with no modules) and
 * installs the contents into the model passed to it.<br>
 * <br>
 * All you need to do is create the model file and set its location to
 * DEFAULT_LOCATION<br>
 * <br>
 * If your module has parameters (implements IParameterized), you can set the
 * default values via createParameterMap()
 */
public class TWMParticipant extends BasicASTParticipant {

	/**
	 * default location of the model content to import or trim
	 */
	private static final String DEFAULT_LOCATION = "de/monochromata/jactr/twm/twm.jactr";

	public static final String CONCEPUTAL_SCHEMA_CHUNK_TYPE = "conceptualSchemaChunkType";
	public static final String DEFAULT_CONCEPUTAL_SCHEMA_CHUNK_TYPE = "tlsConceptualSchema";
	public static final String LEXICALIZED_CONCEPUTAL_SCHEMA_CHUNK_TYPE = "lexicalizedConceptualSchemaChunkType";
	public static final String DEFAULT_LEXICALIZED_CONCEPUTAL_SCHEMA_CHUNK_TYPE = "tlsLexicalizedConceptualSchema";
	public static final String WORD_CHUNK_TYPE = "wordChunkType";
	public static final String DEFAULT_WORD_CHUNK_TYPE = "tlsWord";
	public static final String REFERENCE_POTENTIAL_CHUNK_TYPE = "referencePotentialChunkType";
	public static final String DEFAULT_REFERENCE_POTENTIAL_CHUNK_TYPE = "tlsReferencePotential";
	
	public static final String SCHEMA_CREATION_DURATION_S = "schemaCreationDurationS";
	public static final double DEFAULT_SCHEMA_CREATION_DURATION_S = 0.05;
	public static final String TWM_BUFFER_CAPACITY = "twmBufferCapacity";
	public static final String TWM_GRAPHEMIC_BUFFER_CAPACITY = "twmGraphemicBufferCapacity";
	public static final int DEFAULT_BUFFER_CAPACITY = 7;
	
	/**
	 * must be a zero arg constructor
	 */
	public TWMParticipant() {
		super(TWMParticipant.class.getClassLoader().getResource(
				DEFAULT_LOCATION));
		setInstallableClass(TWMModule.class);
		setParameterMap(createParameterMap());
	}

	public static Map<String, String> createParameterMap() {
		TreeMap<String, String> parameters = new TreeMap<String, String>();
		parameters.put(CONCEPUTAL_SCHEMA_CHUNK_TYPE,
				DEFAULT_CONCEPUTAL_SCHEMA_CHUNK_TYPE);
		parameters.put(LEXICALIZED_CONCEPUTAL_SCHEMA_CHUNK_TYPE,
				DEFAULT_LEXICALIZED_CONCEPUTAL_SCHEMA_CHUNK_TYPE);
		parameters.put(WORD_CHUNK_TYPE,
				DEFAULT_WORD_CHUNK_TYPE);
		parameters.put(REFERENCE_POTENTIAL_CHUNK_TYPE,
				DEFAULT_REFERENCE_POTENTIAL_CHUNK_TYPE);
		
		parameters.put(SCHEMA_CREATION_DURATION_S,
				""+DEFAULT_SCHEMA_CREATION_DURATION_S);
		parameters.put(TWM_BUFFER_CAPACITY,
				""+DEFAULT_BUFFER_CAPACITY);
		parameters.put(TWM_GRAPHEMIC_BUFFER_CAPACITY,
				""+DEFAULT_BUFFER_CAPACITY);
		
		return parameters;
	}
}
