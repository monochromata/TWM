package de.monochromata.jactr.tls;


public class Word extends Schema {

	private final String graphemic;
	private final SpatialInfo spatial;
	private AnaphorInfo anaphorInfo;
	
	public Word(String id, Scope scope,
			String graphemic, SpatialInfo spatial, AnaphorInfo anaphorInfo) {
		this(id, "Word", scope, graphemic, spatial, anaphorInfo);
	}
	
	public Word(String id, String schemaType, Scope scope,
			String graphemic, SpatialInfo spatial, AnaphorInfo anaphorInfo) {
		super(id, schemaType, scope);
		if(graphemic == null)
			throw new IllegalArgumentException("Null graphemic");
		if(spatial == null)
			throw new IllegalArgumentException("Null spatial");
		this.graphemic = graphemic;
		this.spatial = spatial;
		this.anaphorInfo = anaphorInfo;
	}

	public String getGraphemic() {
		return graphemic;
	}

	public SpatialInfo getSpatial() {
		return spatial;
	}

	public AnaphorInfo getAnaphorInfo() {
		return anaphorInfo;
	}

	public void setAnaphorInfo(AnaphorInfo anaphorInfo) {
		this.anaphorInfo = anaphorInfo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((anaphorInfo == null) ? 0 : anaphorInfo.hashCode());
		result = prime * result
				+ ((graphemic == null) ? 0 : graphemic.hashCode());
		result = prime * result + ((spatial == null) ? 0 : spatial.hashCode());
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
		Word other = (Word) obj;
		if (anaphorInfo == null) {
			if (other.anaphorInfo != null)
				return false;
		} else if (!anaphorInfo.equals(other.anaphorInfo))
			return false;
		if (graphemic == null) {
			if (other.graphemic != null)
				return false;
		} else if (!graphemic.equals(other.graphemic))
			return false;
		if (spatial == null) {
			if (other.spatial != null)
				return false;
		} else if (!spatial.equals(other.spatial))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Word [graphemic=" + graphemic + ", spatial=" + spatial
				+ ", anaphorInfo=" + anaphorInfo + "]";
	}

	
	
}
