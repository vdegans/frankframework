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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.dom.DeferredDocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map.Entry;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.ValueResolver;

import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;

public enum DomdocValueResolver implements ValueResolver {
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

		if(context instanceof Node) {
			Node node = ((Node)context).getFirstChild();

			if(name.startsWith("@") && node != null) {
				Node attribute = node.getAttributes().getNamedItem(name.substring(1));
				if(attribute != null) {
					return attribute.getNodeValue();
				}
			}

			while(node != null) {
				String nodeName = node.getNodeName();
				if(name.equals(nodeName)) {
					Node childNode = node.getFirstChild();
					String textNodeValue = null;
					while(childNode != null) {
						String childNodeName = childNode.getNodeName();
						if(childNode.getNodeType() == Node.TEXT_NODE) {
							textNodeValue = childNode.getNodeValue();
						}
						if(childNode.getNodeType() == Node.ELEMENT_NODE) {
							value = node;
							break;
						}

						childNode = childNode.getNextSibling();
					}

					if(value == null) {
						value = textNodeValue;
					}

					break;
				}

				node = node.getNextSibling();
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
		if (context instanceof Node) {
			return context;
		}

		return UNRESOLVED;
	}

//	protected Object resolve2(final Object context, final String name) {
//		if(!(context instanceof Node)) {
//			return UNRESOLVED;
//		}
//		
//		Node node = (Node)context;
//		node.normalize();
//		
//		String nodeName = node.getNodeName();
//		if(!name.equals(nodeName)) {
//			return UNRESOLVED;
//		}
//		
//
//		for (Node child = ((Node)context).getFirstChild(); child != null && child.get; child = child.getNextSibling()) {
//			String childName = child.getNodeName();
//			if(child.getNodeType() == Node.TEXT_NODE) {
//				return child.getNodeValue();
//			} 
//			else if(child.getNodeType() == Node.ELEMENT_NODE) {	
//				return child;
//			}
//
//
//		}
//		
//		
//		return result;
//	}

	/**
	   * List all the properties and their values for the given object.
	   *
	   * @param context The context object. Not null.
	   * @return All the properties and their values for the given object.
	   */
	@Override
	public Set<Entry<String, Object>> propertySet(final Object context) {
		return Collections.emptySet();
	}
}
