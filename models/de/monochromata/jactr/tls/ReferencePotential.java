package de.monochromata.jactr.tls;


public class ReferencePotential extends Word {

	private final boolean isDefinite;
	private Object referent;
	private final String coReferenceChain, declaredIn, schema, roleIn;
	private String roleId;
	private String returnId;
	
	public ReferencePotential(String id, Scope scope,
			String graphemic, SpatialInfo spatial, AnaphorInfo anaphorInfo, boolean isDefinite, Object referent,
			String coReferenceChain, String declaredIn, String schema, String roleIn,
			String roleId, String returnId) {
		super(id, "ReferencePotential", scope, graphemic, spatial, anaphorInfo);
		this.isDefinite = isDefinite;
		this.referent = referent;
		this.coReferenceChain = coReferenceChain;
		this.declaredIn = declaredIn;
		this.schema = schema;
		this.roleIn = roleIn;
		this.roleId = roleId;
		this.returnId = returnId;
	}

	public boolean isDefinite() {
		return isDefinite;
	}

	public Object getReferent() {
		return referent;
	}

	public void setReferent(Object referent) {
		this.referent = referent;
	}

	public String getCoReferenceChain() {
		return coReferenceChain;
	}

	public String getDeclaredIn() {
		return declaredIn;
	}

	/**
	 * If the reference potential belongs to the declaration of a schema,
	 * the ID of that schema is returned by this method.
	 * 
	 * @return A schema ID or null
	 * @see LexicalizedConceptualSchema
	 */
	public String getSchema() {
		return schema;
	}

	public String getRoleIn() {
		return roleIn;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	
	public String getReturnId() {
		return returnId;
	}

	public void setReturnId(String returnId) {
		this.returnId = returnId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((coReferenceChain == null) ? 0 : coReferenceChain.hashCode());
		result = prime * result
				+ ((declaredIn == null) ? 0 : declaredIn.hashCode());
		result = prime * result + (isDefinite ? 1231 : 1237);
		result = prime * result
				+ ((referent == null) ? 0 : referent.hashCode());
		result = prime * result
				+ ((returnId == null) ? 0 : returnId.hashCode());
		result = prime * result + ((roleId == null) ? 0 : roleId.hashCode());
		result = prime * result + ((roleIn == null) ? 0 : roleIn.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
		ReferencePotential other = (ReferencePotential) obj;
		if (coReferenceChain == null) {
			if (other.coReferenceChain != null)
				return false;
		} else if (!coReferenceChain.equals(other.coReferenceChain))
			return false;
		if (declaredIn == null) {
			if (other.declaredIn != null)
				return false;
		} else if (!declaredIn.equals(other.declaredIn))
			return false;
		if (isDefinite != other.isDefinite)
			return false;
		if (referent == null) {
			if (other.referent != null)
				return false;
		} else if (!referent.equals(other.referent))
			return false;
		if (returnId == null) {
			if (other.returnId != null)
				return false;
		} else if (!returnId.equals(other.returnId))
			return false;
		if (roleId == null) {
			if (other.roleId != null)
				return false;
		} else if (!roleId.equals(other.roleId))
			return false;
		if (roleIn == null) {
			if (other.roleIn != null)
				return false;
		} else if (!roleIn.equals(other.roleIn))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ReferencePotential [isDefinite=" + isDefinite + ", referent="
				+ referent + ", coReferenceChain=" + coReferenceChain
				+ ", declaredIn=" + declaredIn + ", schema=" + schema
				+ ", roleIn=" + roleIn + ", roleId=" + roleId + ", returnId="
				+ returnId + "]";
	}




}
