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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.HighConcurrencyTemplateCache;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;

import lombok.Getter;
import lombok.Setter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Resource;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;


public class HandlebarsEngine implements InitializingBean {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter String name;
	private @Getter @Setter ApplicationContext applicationContext = null;
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	protected Handlebars handlebars = null;

	protected static final String TEMPLATES_DIRS = AppConstants.getInstance().getProperty("handlebars.templates.dirs", "/");
	protected static final String TEMPLATES_DEFAULT_SUFFIX = ".hbs";

	public HandlebarsEngine() {

	}

	@Override
	public void afterPropertiesSet() throws ConfigurationException {
		List<TemplateLoader> templateLoaders = new ArrayList<TemplateLoader>();

		StringTokenizer tokenizer = new StringTokenizer(TEMPLATES_DIRS, ";");
		while (tokenizer.hasMoreTokens()) {
			String dir = tokenizer.nextToken();
			templateLoaders.add(new ClassPathTemplateLoader(dir, TEMPLATES_DEFAULT_SUFFIX));
			if(log.isTraceEnabled()) log.trace("["+getName()+"] " +" added ClassPathTemplateLoader for ["+dir+"]");
		}

		if(templateLoaders.isEmpty()) {
			log.warn(getLogPrefix() + "no template repository configured; defaulting to classpath root");
		}

		handlebars = new Handlebars()
				.with(templateLoaders.toArray(new TemplateLoader[] {}))
				.infiniteLoops(true)
				.with(new HighConcurrencyTemplateCache())
				.registerHelpers(com.github.jknack.handlebars.helper.StringHelpers.class)
				.registerHelpers(com.github.jknack.handlebars.helper.ConditionalHelpers.class);
	}

	public Template compile(String templateName) throws IOException {
		return handlebars.compile(templateName);
	}

	public Template compile(Resource template) throws IOException {
		String source = StreamUtil.streamToString(template.openStream(), null, null);

		return compileInline(source);
	}

	public Template compileInline(String templateSource) throws IOException {
		return handlebars.compileInline(templateSource);
	}

	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}
}
