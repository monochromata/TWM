package de.monochromata.jactr.tls;

import java.util.List;
import java.util.Map;

public class LexicalizedConceptualSchema extends ConceptualSchema {

	private final Map<String,Object> lexicalFeatures;
	
	public LexicalizedConceptualSchema(String id, String schemaType, Scope scope,
			double activation, boolean isTechnical, boolean isObject,
			Map<String,Object> lexicalFeatures, List<String> conceptualFeatures) {
		super(id, schemaType, scope, activation, isTechnical, isObject, conceptualFeatures);
		this.lexicalFeatures = lexicalFeatures;
	}

	public Map<String, Object> getLexicalFeatures() {
		return lexicalFeatures;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((lexicalFeatures == null) ? 0 : lexicalFeatures.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LexicalizedConceptualSchema other = (LexicalizedConceptualSchema) obj;
		if (lexicalFeatures == null) {
			if (other.lexicalFeatures != null)
				return false;
		} else if (!lexicalFeatures.equals(other.lexicalFeatures))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LexicalizedConceptualSchema [lexicalFeatures="
				+ lexicalFeatures + ", activation=" + activation
				+ ", isTechnical=" + isTechnical + ", conceptualFeatures="
				+ conceptualFeatures + "]";
	}


}
