package de.monochromata.jactr.tls;

public class Schema {
	
	public static final String TOKEN = "Token";
	public static final String TYPE = "Type";
	
	private final String id, schemaType;
	private final Scope scope;
	
	public Schema(String id, String schemaType, Scope scope) {
		this.id = id;
		this.schemaType = schemaType;
		this.scope = scope;
	}

	public String getId() {
		return id;
	}

	public String getSchemaType() {
		return schemaType;
	}

	public Scope getScope() {
		return scope;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((schemaType == null) ? 0 : schemaType.hashCode());
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Schema other = (Schema) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (schemaType == null) {
			if (other.schemaType != null)
				return false;
		} else if (!schemaType.equals(other.schemaType))
			return false;
		if (scope == null) {
			if (other.scope != null)
				return false;
		} else if (!scope.equals(other.scope))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Schema [id=" + id + ", schemaType=" + schemaType + ", scope="
				+ scope + "]";
	}
	
	
}
