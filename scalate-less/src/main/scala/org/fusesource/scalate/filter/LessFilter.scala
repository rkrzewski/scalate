/*
 * Copyright (C) 2009-2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.scalate.filter

import org.fusesource.scalate.{TemplateEngineAddOn, RenderContext, TemplateEngine}
import org.fusesource.scalate.servlet.ServletRenderContext
import org.fusesource.scalate.util.IOUtil
import com.asual.lesscss.{LessEngine, LessOptions}
import com.asual.lesscss.loader.ResourceLoader
import java.io.IOException

/**
 * Renders Less syntax.
 *
 * @author <a href="mailto:stuart.roebuck@gmail.com">Stuart Roebuck</a>
 */
class LessFilter(private val lessEngine: LessEngine) extends Filter {
  def filter(context: RenderContext, content: String) = {
    val css = synchronized {
    	
      // This code block is synchronized as I'm not confident that the Less filter is thread safe.
      lessEngine.compile(content, context.currentTemplate).stripLineEnd
    }
    if(context.currentTemplate.endsWith(".less")) {
      context.attributes("layout") = ""
      css
    } else {
      // rendering less block embedded in another template
      """<style type="text/css">%n%s%n</style>""".format(css)
    }
  }
}

/**
 * Adds the less filter to the template engine.
 */
object LessFilterAddOn extends TemplateEngineAddOn {
  def apply(te: TemplateEngine) {
    val lessEngine = new LessEngine(new LessOptions, new ScalateResourceLoader(te))
    val lessFilter = new LessFilter(lessEngine)
    te.filters += "less" -> lessFilter
    te.pipelines += "less" -> List(NoLayoutFilter(lessFilter, "text/css"))
    te.templateExtensionsFor("css") += "less"
  }
}

/**
 * Less ResourceLoader for processing includes
 *
 * @author Rafał Krzewski
 */
class ScalateResourceLoader(private val engine: TemplateEngine) extends ResourceLoader {
  def exists(path: String): Boolean = {
    engine.resourceLoader.resource(path).isDefined
  }

  def load(path: String, charset: String): String = {
    engine.resourceLoader.resource(path) match {
      case Some(r) =>
        IOUtil.loadText(r.inputStream, charset)
      case _ =>
        throw new IOException("No such file " + path)
    }
  }
}
