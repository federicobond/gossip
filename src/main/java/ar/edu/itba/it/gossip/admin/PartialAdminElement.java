package ar.edu.itba.it.gossip.admin;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;
import static ar.edu.itba.it.gossip.util.ValidationUtils.require;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.util.PartiallySerializable;

public class PartialAdminElement implements PartiallySerializable {
	public static PartialAdminElement from(PartialXMLElement element) {
		return new PartialAdminElement(element);
	}

	private final PartialXMLElement xmlElement;
	private final Type type;

	PartialAdminElement(final PartialXMLElement xmlElement) {
		this.xmlElement = xmlElement;
		this.type = Type.of(xmlElement.getName());
	}

	public PartialXMLElement getXML() {
		return xmlElement;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String serializeCurrentContent() {
		String serialization = xmlElement.serializeCurrentContent();

		return serialization;
	}

	@Override
	public String toString() {
		return reflectionToString(this);
	}

	public enum Type {
		USER("usr"), PASS("pass"), LEET("leet"), SILENCE("silence"), ORIGIN(
				"origin"), STATS("stats"), QUIT("quit"), OTHER;

		private static final Map<String, Type> typesByName;

		static {
			// NOTE: This is here solely because of the really limited access to
			// static fields Java enums have

			Map<String, Type> typesMap = new HashMap<>();
			for (Type type : values()) {
				if (type.name.isPresent()) {
					assumeState(!typesMap.containsKey(type.name.get()),
							"Name %s is repeated");
					typesMap.put(type.name.get(), type);
				}
			}
			typesByName = unmodifiableMap(typesMap);
		}

		private final Optional<String> name;

		Type() {
			this.name = Optional.empty();
		}

		Type(String name) {
			this.name = Optional.of(name);
		}

		static Type of(String name) {
			require(name != null);
			return typesByName.getOrDefault(name, OTHER);
		}
	}

}
