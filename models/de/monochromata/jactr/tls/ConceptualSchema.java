package de.monochromata.jactr.tls;

import java.util.List;

public class ConceptualSchema extends Schema {

	protected double activation;
	protected boolean isTechnical, isObject;
	protected final List<String> conceptualFeatures;

	public ConceptualSchema(String id, String schemaType, Scope scope,
			double activation, boolean isTechnical, boolean isObject,
			List<String> conceptualFeatures) {
		super(id, schemaType, scope);
		this.activation = activation;
		this.isTechnical = isTechnical;
		this.isObject = isObject;
		this.conceptualFeatures = conceptualFeatures;
	}

	public double getActivation() {
		return activation;
	}

	public void setActivation(double activation) {
		this.activation = activation;
	}

	public boolean isTechnical() {
		return isTechnical;
	}

	public void setTechnical(boolean isTechnical) {
		this.isTechnical = isTechnical;
	}

	public boolean isObject() {
		return isObject;
	}

	public void setObject(boolean isObject) {
		this.isObject = isObject;
	}

	public List<String> getConceptualFeatures() {
		return conceptualFeatures;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(activation);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime
				* result
				+ ((conceptualFeatures == null) ? 0 : conceptualFeatures
						.hashCode());
		result = prime * result + (isObject ? 1231 : 1237);
		result = prime * result + (isTechnical ? 1231 : 1237);
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
		ConceptualSchema other = (ConceptualSchema) obj;
		if (Double.doubleToLongBits(activation) != Double
				.doubleToLongBits(other.activation))
			return false;
		if (conceptualFeatures == null) {
			if (other.conceptualFeatures != null)
				return false;
		} else if (!conceptualFeatures.equals(other.conceptualFeatures))
			return false;
		if (isObject != other.isObject)
			return false;
		if (isTechnical != other.isTechnical)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ConceptualSchema [activation=" + activation + ", isTechnical="
				+ isTechnical + ", isObject=" + isObject
				+ ", conceptualFeatures=" + conceptualFeatures + "]";
	}
	
	

}