/*
 * Sonar .NET Plugin :: Gendarme
 * Copyright (C) 2010 Jose Chillan, Alexandre Victoor and SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.csharp.gendarme;

import org.junit.Test;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.plugins.dotnet.api.DotNetConstants;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GendarmeRuleRepositoryProviderTest {

  @SuppressWarnings("unchecked")
  @Test
  public void shouldCreateRepositoriesForEachSupportedLanguage() {
    ServerFileSystem fileSystem = mock(ServerFileSystem.class);
    GendarmeRuleRepositoryProvider provider = new GendarmeRuleRepositoryProvider(fileSystem, null);

    List<GendarmeRuleRepository> extensions = (List<GendarmeRuleRepository>) provider.provide();
    assertThat(extensions.size()).isEqualTo(2);

    GendarmeRuleRepository repo1 = extensions.get(0);
    assertThat(repo1.getLanguage()).isEqualTo(DotNetConstants.CSHARP_LANGUAGE_KEY);
    assertThat(repo1.getKey()).isEqualTo("gendarme");

    GendarmeRuleRepository repo2 = extensions.get(1);
    assertThat(repo2.getLanguage()).isEqualTo(DotNetConstants.VBNET_LANGUAGE_KEY);
    assertThat(repo2.getKey()).isEqualTo("gendarme-vbnet");
  }
}
