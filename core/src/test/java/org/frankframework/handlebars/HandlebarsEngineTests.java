package org.frankframework.handlebars;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.jknack.handlebars.Context;

import com.github.jknack.handlebars.JsonNodeValueResolver;

import org.frankframework.collection.CollectionException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.Template;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.PipeRunResult;
import org.frankframework.handlebars.HandlebarsEngine;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.util.XmlUtils;

public class HandlebarsEngineTests extends ConfiguredTestBase{

	public HandlebarsEngine createHandlebarsEngine() throws BeansException {
		HandlebarsEngine he = new HandlebarsEngine();
		this.autowireByType(he);

		return he;
	}

	@Test()
	public void testCompileInlineWithNonEmptySourceShouldReturnValidTemplate() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();
		Template template = null;

		template = he.compileInline("{{test}}");

		assertNotNull(template);
		assertEquals("test", template.collect(TagType.VAR).get(0));
	}

	@Test()
	public void testCompileInlineWithEmptySourceReturnsValidTemplate() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();
		Template template = null;

		template = he.compileInline("");

		assertNotNull(template);
		assertEquals(Collections.EMPTY_LIST, template.collect(TagType.VAR));
	}

	@Test()
	public void testCompileInlineWithNullSourceThrowsNullPointerException() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();

		assertThrows(NullPointerException.class, () -> he.compileInline(null));
	}

	@Test
	public void testCompileInlineWithMalformedTemplate() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();

		assertThrows(Exception.class, () -> he.compileInline("{{#if}}"));
	}

	@Test
	public void testCompileInlineWithComplexTemplate() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();
		Template template = he.compileInline("{{#if test}}{{value1}}{{value2}}{{/if}}");

		assertNotNull(template);
		assertEquals(2, template.collect(TagType.VAR).size());
		assertEquals(1, template.collect(TagType.SECTION).size());
	}

	@Test
	public void testTemplateRenderingWithContext() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();
		Template template = he.compileInline("Hello, {{name}}!");

		Map<String, Object> context = new HashMap<>();
		context.put("name", "World");

		String output = template.apply(context);

		assertEquals("Hello, World!", output);
	}

	@Test
	public void rootShouldNotConflict() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();
		Template template = he.compileInline("Hello, {{root.name}}!");


		Document dom = XmlUtils.buildDomDocument("<root><name>World</name></root>", false);
		Context context = Context.newBuilder(dom)
				.resolver(DomdocValueResolver.INSTANCE)
				.build();

		Context pvlContext = Context.newBuilder("")
				.resolver(ParameterListValueResolver.INSTANCE, DomdocValueResolver.INSTANCE)
				.build();

		Context.newBuilder(context).resolver(ParameterListValueResolver.INSTANCE, DomdocValueResolver.INSTANCE, JsonNodeValueResolver.INSTANCE);

		Context ctx = Context.newBuilder(context, pvlContext.model())
				.resolver(ParameterListValueResolver.INSTANCE, DomdocValueResolver.INSTANCE, JsonNodeValueResolver.INSTANCE)
				.build();

		String output = template.apply(context);

		assertEquals("Hello, World!", output);
	}

	@Test
	public void testTemplateRenderingWithMissingVariable() throws Exception {
		HandlebarsEngine he = createHandlebarsEngine();
		Template template = he.compileInline("Hello, {{name}}!");

		String output = template.apply(Collections.emptyMap());

		assertEquals("Hello, !", output);
	}
}
