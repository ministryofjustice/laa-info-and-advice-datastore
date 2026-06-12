package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;

/** Unit tests for the {@link ApplicationService}. */
@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {
  @Mock private ApplicationRepository repo;
  @Mock private ApplicationMapper mapper;
  @Mock private UserContext userContext;

  @InjectMocks private ApplicationService sut;

  @Test
  void shouldCreateApplication() {
    // Arrange
    final StartCaseCommand cmd = new StartCaseCommand();
    final ApplicationEntity entity = new ApplicationEntity();
    final UUID generatedId = UUID.randomUUID();
    final UUID firmId = UUID.randomUUID();
    final UUID officeId = UUID.randomUUID();
    final String user = "TEST_USER";

    when(mapper.toApplicationEntity(cmd)).thenReturn(entity);
    when(userContext.getProviderFirmId()).thenReturn(firmId);
    when(userContext.getProviderOfficeId()).thenReturn(officeId);
    when(userContext.getCurrentUser()).thenReturn(user);
    when(repo.save(any(ApplicationEntity.class)))
        .thenAnswer(
            invocation -> {
              ApplicationEntity saved = invocation.getArgument(0);
              saved.setId(generatedId);
              return saved;
            });

    // Act
    final UUID result = sut.createApplication(cmd);

    // Assert
    assertThat(result).isEqualTo(generatedId);
    assertThat(entity.getProviderFirmId()).isEqualTo(firmId);
    assertThat(entity.getProviderOfficeId()).isEqualTo(officeId);
    assertThat(entity.getApplicationState()).isEqualTo(ApplicationState.DRAFT);
    assertThat(entity.getCreatedBy()).isEqualTo(user);
    assertThat(entity.getModifiedBy()).isEqualTo(user);

    verify(repo, times(1)).save(entity);
  }

  @Test
  void shouldGetAllApplications() {
    // Arrange
    final ApplicationEntity entity1 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationEntity entity2 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application1 =
        ApplicationResponse.builder().id(entity1.getId()).build();
    final ApplicationResponse application2 =
        ApplicationResponse.builder().id(entity2.getId()).build();
    when(repo.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity1, entity2)));
    when(mapper.toApplication(entity1)).thenReturn(application1);
    when(mapper.toApplication(entity2)).thenReturn(application2);

    // Act
    final List<ApplicationResponse> result = sut.getAllApplications(null, null);

    // Assert
    assertThat(result).hasSize(2).contains(application1, application2);
    verify(repo, times(1)).findAll(any(org.springframework.data.domain.Pageable.class));
    verify(mapper, times(2)).toApplication(any());
  }

  @Test
  void shouldGetApplication() {
    // Arrange
    final ApplicationEntity entity = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application =
        ApplicationResponse.builder().id(entity.getId()).build();
    when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(mapper.toApplication(entity)).thenReturn(application);

    // Act
    final ApplicationResponse result = sut.getApplication(entity.getId()).orElseThrow();

    // Assert
    assertThat(result).isEqualTo(application);
    verify(repo, times(1)).findById(entity.getId());
    verify(mapper, times(1)).toApplication(entity);
  }

  @Test
  void shouldReturnEmptyOptional_whenApplicationDoesNotExist() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    Optional<ApplicationResponse> result = sut.getApplication(UUID.randomUUID());

    // Assert
    assertTrue(result.isEmpty());
  }
}
