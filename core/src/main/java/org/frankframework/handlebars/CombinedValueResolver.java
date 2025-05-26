package org.frankframework.handlebars;

import com.github.jknack.handlebars.ValueResolver;

import org.frankframework.handlebars.DomdocValueResolver;
import org.frankframework.handlebars.ParameterListValueResolver;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.github.jknack.handlebars.ValueResolver.UNRESOLVED;

public enum CombinedValueResolver implements ValueResolver {
	INSTANCE;

	@Override
	public Object resolve(Object context, String name) {
		if (!(context instanceof Map)) return UNRESOLVED;
		Map<?, ?> map = (Map<?, ?>) context;

		Object domdoc = map.get("domdoc");
		Object params = map.get("params");

		Object value = DomdocValueResolver.INSTANCE.resolve(domdoc, name);
		if (value != UNRESOLVED) return value;

		value = ParameterListValueResolver.INSTANCE.resolve(params, name);
		return value;
	}

	@Override
	public Object resolve(Object context) {
		return UNRESOLVED;
	}

	@Override
	public Set<Map.Entry<String, Object>> propertySet(Object context) {
		return Collections.emptySet(); // Optional: implement if needed
	}
}
