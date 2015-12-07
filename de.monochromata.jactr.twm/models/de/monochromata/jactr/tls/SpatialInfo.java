package de.monochromata.jactr.tls;

public class SpatialInfo {
	
	private final String uri;
	private final int line, column, length;
	
	public SpatialInfo(String uri, int line, int column, int length) {
		if(uri == null)
			throw new IllegalArgumentException("Null uri");
		this.uri = uri;
		this.line = line;
		this.column = column;
		this.length = length;
	}

	public String getUri() {
		return uri;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public int getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + column;
		result = prime * result + length;
		result = prime * result + line;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		SpatialInfo other = (SpatialInfo) obj;
		if (column != other.column)
			return false;
		if (length != other.length)
			return false;
		if (line != other.line)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SpatialInfo [uri=" + uri + ", line=" + line + ", column="
				+ column + ", length=" + length + "]";
	}
	
}
