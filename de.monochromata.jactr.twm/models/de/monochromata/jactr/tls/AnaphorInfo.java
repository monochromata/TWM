package de.monochromata.jactr.tls;

public class AnaphorInfo {

	private final int id;
	private final String condition,
						 daia,
						 kind,
						 relationActivationLevel,
						 uri;
	private final int line, column;
	private final String word;
	
	public AnaphorInfo(int id, String condition, String daia, String kind,
			String relationActivationLevel, String uri, int line, int column,
			String word) {
		this.id = id;
		this.condition = condition;
		this.daia = daia;
		this.kind = kind;
		this.relationActivationLevel = relationActivationLevel;
		this.uri = uri;
		this.line = line;
		this.column = column;
		this.word = word;
	}

	public int getId() {
		return id;
	}

	public String getCondition() {
		return condition;
	}

	public String getDaia() {
		return daia;
	}

	public String getKind() {
		return kind;
	}

	public String getRelationActivationLevel() {
		return relationActivationLevel;
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

	public String getWord() {
		return word;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + column;
		result = prime * result
				+ ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((daia == null) ? 0 : daia.hashCode());
		result = prime * result + id;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + line;
		result = prime
				* result
				+ ((relationActivationLevel == null) ? 0
						: relationActivationLevel.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((word == null) ? 0 : word.hashCode());
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
		AnaphorInfo other = (AnaphorInfo) obj;
		if (column != other.column)
			return false;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (daia == null) {
			if (other.daia != null)
				return false;
		} else if (!daia.equals(other.daia))
			return false;
		if (id != other.id)
			return false;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (line != other.line)
			return false;
		if (relationActivationLevel == null) {
			if (other.relationActivationLevel != null)
				return false;
		} else if (!relationActivationLevel
				.equals(other.relationActivationLevel))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AnaphorInfo [id=" + id + ", condition=" + condition + ", daia="
				+ daia + ", kind=" + kind + ", relationActivationLevel="
				+ relationActivationLevel + ", uri=" + uri + ", line=" + line
				+ ", column=" + column + ", word=" + word + "]";
	}
	
	
}
