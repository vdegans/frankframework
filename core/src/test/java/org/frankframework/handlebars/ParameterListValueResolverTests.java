package org.frankframework.handlebars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.frankframework.parameters.AbstractParameter;
import org.frankframework.parameters.BooleanParameter;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterType;

import org.frankframework.parameters.ParameterValue;

import org.junit.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Context;

import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.util.XmlUtils;

import ujson.Bool;

public class ParameterListValueResolverTests {

	@Test()
	public void nonParameterValueListTypeShouldReturnUnresolved() throws Exception {
		Parameter notAParameterValueList = new Parameter();
		ParameterListValueResolver resolver = ParameterListValueResolver.INSTANCE;

		Object result = resolver.resolve(notAParameterValueList, "test");

		assertSame(result, resolver.UNRESOLVED);
	}

	@Test()
	public void emptyParameterValueListTypeShouldReturnUnresolved() throws Exception {
		ParameterList pl = new ParameterList();
		ParameterValueList pvl = ParameterBuilder.getPVL(pl);
		ParameterListValueResolver resolver = ParameterListValueResolver.INSTANCE;

		Object result = resolver.resolve(pvl, "test");

		assertSame(result, resolver.UNRESOLVED);
	}

	@Test()
	public void StringParameterShouldReturnStringValue() throws Exception {
		ParameterList pl = new ParameterList();
		pl.add(ParameterBuilder.create().withName("test").withValue("value").withType(ParameterType.STRING));
		ParameterValueList pvl = ParameterBuilder.getPVL(pl);

		Context context = Context
				.newBuilder(pvl)
				.resolver(ParameterListValueResolver.INSTANCE)
				.build();

		Object result = context.get("test");

		assertNotNull(context);
		assertTrue(result instanceof String);
		assertEquals("value", result);
	}

	@Test()
	public void BooleanParameterShouldReturnBooleanValue() throws Exception {
		ParameterList pl = new ParameterList();
		BooleanParameter booleanParam = new BooleanParameter();
		booleanParam.setName("test");
		booleanParam.setValue("true");
		pl.add(booleanParam);
		IParameter param = pl.getParameter(0);
		ParameterValueList pvl = ParameterBuilder.getPVL(pl);

		Context context = Context
				.newBuilder(pvl)
				.resolver(ParameterListValueResolver.INSTANCE)
				.build();

		param.getValue();

		ParameterListValueResolver resolver = ParameterListValueResolver.INSTANCE;
		Object result = resolver.resolve(pvl, "test");

		assertNotNull(context);
		assertTrue(result instanceof Boolean);
		assertEquals(true, result);
	}

	@Test()
	public void NodeParameterShouldReturnNodeObject() throws Exception {
		ParameterList pl = new ParameterList();
		pl.add(ParameterBuilder.create().withName("test").withValue("<root>value</root>").withType(ParameterType.DOMDOC));
		ParameterValueList pvl = ParameterBuilder.getPVL(pl);
		ParameterListValueResolver resolver = ParameterListValueResolver.INSTANCE;

		Object result = resolver.resolve(pvl, "test");

		assertInstanceOf(Document.class, result);
		Document document = (Document)result;
		assertEquals("#document", document.getNodeName());
		assertEquals("root", document.getFirstChild().getNodeName());
		assertEquals("value", document.getFirstChild().getFirstChild().getNodeValue());

	}

	@Test()
	public void DomdocParameterShouldReturnDomdocObject() throws Exception {
		ParameterList pl = new ParameterList();
		pl.add(ParameterBuilder.create().withName("test").withValue("<root>value</root>").withType(ParameterType.DOMDOC));
		ParameterValueList pvl = ParameterBuilder.getPVL(pl);
		ParameterListValueResolver resolver = ParameterListValueResolver.INSTANCE;

		Object result = resolver.resolve(pvl, "test");

		assertInstanceOf(Node.class, result);
		assertEquals("root", ((Node)result).getFirstChild().getNodeName());
		assertEquals("value", ((Node)result).getFirstChild().getFirstChild().getNodeValue());
	}

	@Test()
	public void JsonParameterShouldReturnJsonObject() throws Exception {
		ParameterList pl = new ParameterList();

		pl.add(ParameterBuilder.create().withName("test").withValue("{\"firstName\":\"John\", \"lastName\":\"Smith\"}").withType(ParameterType.STRING));
		ParameterValueList pvl = ParameterBuilder.getPVL(pl);
		ParameterListValueResolver resolver = ParameterListValueResolver.INSTANCE;

		Object result = resolver.resolve(pvl, "test");

		assertInstanceOf(com.fasterxml.jackson.databind.node.ObjectNode.class, result);
		assertEquals("{\"firstName\":\"John\",\"lastName\":\"Smith\"}", result.toString());
	}

}
