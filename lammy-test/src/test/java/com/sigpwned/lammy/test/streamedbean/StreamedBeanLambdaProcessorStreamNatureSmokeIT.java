/*-
 * =================================LICENSE_START==================================
 * lammy-core
 * ====================================SECTION=====================================
 * Copyright (C) 2023 - 2025 Andy Boothe
 * ====================================SECTION=====================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================LICENSE_END===================================
 */
package com.sigpwned.lammy.test.streamedbean;

import org.junit.jupiter.api.Disabled;
import com.sigpwned.lammy.core.base.streamedbean.StreamedBeanLambdaProcessorBase;
import com.sigpwned.lammy.core.model.bean.ExceptionMapper;
import com.sigpwned.lammy.core.model.bean.RequestFilter;
import com.sigpwned.lammy.core.model.bean.ResponseFilter;
import com.sigpwned.lammy.core.model.stream.ExceptionWriter;
import com.sigpwned.lammy.test.StreamSmokeTestBase;

/**
 * {@link StreamedBeanLambdaProcessorBase} supports {@link RequestFilter}, {@link ResponseFilter},
 * and {@link ExceptionMapper}, so {@link StreamedBeanLambdaProcessorBeanNatureSmokeIT} works.
 * However, {@link StreamedBeanLambdaProcessorBase} does not support {@link ExceptionWriter}, so
 * {@link StreamedBeanLambdaProcessorStreamNatureSmokeIT} does not work.
 */
@Disabled("Do not support ExceptionWriter")
public class StreamedBeanLambdaProcessorStreamNatureSmokeIT extends StreamSmokeTestBase {
  @Override
  public String greetingProcessorSource(Boolean autoloadAll, Boolean autoloadInputInterceptors,
      Boolean autoloadOutputInterceptors, Boolean autoloadExceptionMappers) {
    throw new UnsupportedOperationException();
  }
}
