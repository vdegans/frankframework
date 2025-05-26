/*
Copyright 2023-2023 WeAreFrank!

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.frankframework.handlebars;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;

import org.apache.xerces.dom.DeferredDocumentImpl;

import org.frankframework.parameters.AbstractParameter;

import org.frankframework.util.DomBuilderException;
import org.frankframework.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map.Entry;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.ValueResolver;

import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;

public enum ParameterListValueResolver implements ValueResolver {
	/**
	   * The singleton instance.
	   */
	INSTANCE;

	/**
	   * Resolve the attribute's name in the context object. If a {@link #UNRESOLVED} is returned, the
	   * {@link Context context stack} will
	   * continue with the next value resolver in the chain.
	   *
	   * @param context The context object. Not null.
	   * @param name The attribute's name. Not null.
	   * @return A {@link #UNRESOLVED} is returned, the {@link Context context
	   *         stack} will continue with the next value resolver in the chain.
	   *         Otherwise, it returns the associated value.
	   */
	@Override
	public Object resolve(final Object context, final String name) {
		Object value = null;

		if (context instanceof ParameterValueList) {
			ParameterValue pv = null;
			pv = ((ParameterValueList) context).get(name);


			if(pv != null) {
				value = pv.getValue();

				if (value != null) {
					JsonNode jsonNode = null;
					try {
						jsonNode = new ObjectMapper().readValue((String) value, JsonNode.class);
					} catch (Exception ignored) {

					}

					if (jsonNode != null) {
						return jsonNode;
					}

					Document domdoc = null;
					try {
						domdoc = XmlUtils.buildDomDocument((String) value, false);
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (domdoc != null) {
						return domdoc;
					}
				}
			}
		}

		return value == null ? UNRESOLVED : value;
	}

	/**
	   * Resolve the the context object by optionally converting the value if necessary.
	   * If a {@link #UNRESOLVED} is returned, the {@link Context context stack} will continue with
	   * the next value resolver in the chain.
	   *
	   * @param context The context object. Not null.
	   * @return A {@link #UNRESOLVED} is returned, the {@link Context context
	   *         stack} will continue with the next value resolver in the chain.
	   *         Otherwise, it returns the associated value.
	   */
	public Object resolve(final Object context) {
		if (context instanceof ParameterValueList) {
			return context;
		}

		return UNRESOLVED;
	}

	/**
	   * List all the properties and their values for the given object.
	   *
	   * @param context The context object. Not null.
	   * @return All the properties and their values for the given object.
	   */
	@Override
	public Set<Entry<String, Object>> propertySet(final Object context) {
		if (context instanceof ParameterValueList) {
			return ((ParameterValueList) context).getValueMap().entrySet();
		}

		return Collections.emptySet();
	}
}
