/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.resource.api.free;

import com.codenvy.resource.model.ResourceType;
import com.codenvy.resource.shared.dto.FreeResourcesLimitDto;
import com.codenvy.resource.shared.dto.ResourceDto;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Set;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

/**
 * Test for {@link FreeResourcesLimitValidator}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class FreeResourcesLimitValidatorTest {
    private static final String      RESOURCE_TYPE   = "test";
    private static final Set<String> SUPPORTED_UNITS = ImmutableSet.of("mb", "gb");
    @Mock
    private ResourceType resourceType;

    private FreeResourcesLimitValidator validator;

    @BeforeMethod
    public void setUp() throws Exception {
        when(resourceType.getId()).thenReturn(RESOURCE_TYPE);
        when(resourceType.getSupportedUnits()).thenReturn(SUPPORTED_UNITS);

        validator = new FreeResourcesLimitValidator(ImmutableSet.of(resourceType));
    }

    @Test(expectedExceptions = BadRequestException.class,
          expectedExceptionsMessageRegExp = "Missed resources limit description")
    public void shouldThrowBadRequestExceptionWhenResourcesLimitIsNull() throws Exception {
        //when
        validator.check(null);
    }

    @Test(expectedExceptions = BadRequestException.class,
          expectedExceptionsMessageRegExp = "Specified resources type 'unsupported' is not supported")
    public void shouldThrowBadRequestExceptionWhenResourcesLimitContainsResourceWithNonSupportedType() throws Exception {
        //when
        validator.check(DtoFactory.newDto(FreeResourcesLimitDto.class)
                                  .withResources(singletonList(DtoFactory.newDto(ResourceDto.class)
                                                                         .withType("unsupported")
                                                                         .withUnit("mb"))));
    }

    @Test(expectedExceptions = BadRequestException.class,
          expectedExceptionsMessageRegExp = "Specified resources type 'test' support only following units: mb, gb")
    public void shouldThrowBadRequestExceptionWhenResourcesLimitContainsResourceWithNonSupportedUnit() throws Exception {
        //when
        validator.check(DtoFactory.newDto(FreeResourcesLimitDto.class)
                                  .withResources(singletonList(DtoFactory.newDto(ResourceDto.class)
                                                                         .withType(RESOURCE_TYPE)
                                                                         .withUnit("kb"))));
    }

    @Test
    public void shouldNotThrowAnyExceptionsWhenResourcesLimitContainsOnlyResourceWithSupportedTypeAndUnit() throws Exception {
        //when
        validator.check(DtoFactory.newDto(FreeResourcesLimitDto.class)
                                  .withResources(singletonList(DtoFactory.newDto(ResourceDto.class)
                                                                         .withType(RESOURCE_TYPE)
                                                                         .withUnit("mb"))));
    }
}
