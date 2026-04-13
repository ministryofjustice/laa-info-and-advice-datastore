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
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;

/** Unit tests for the {@link ApplicationService}. */
@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {
  @Mock private ApplicationRepository repo;
  @Mock private ApplicationMapper mapper;

  @InjectMocks private ApplicationService sut;

  @Test
  void shouldGetAllApplications() {
    // Arrange
    final ApplicationEntity entity1 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationEntity entity2 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application1 =
        ApplicationResponse.builder().referenceNumber(entity1.getId()).build();
    final ApplicationResponse application2 =
        ApplicationResponse.builder().referenceNumber(entity2.getId()).build();
    when(repo.findAll()).thenReturn(List.of(entity1, entity2));
    when(mapper.toApplication(entity1)).thenReturn(application1);
    when(mapper.toApplication(entity2)).thenReturn(application2);

    // Act
    final List<ApplicationResponse> result = sut.getAllApplications();

    // Assert
    assertThat(result).hasSize(2).contains(application1, application2);
    verify(repo, times(1)).findAll();
    verify(mapper, times(2)).toApplication(any());
  }

  @Test
  void shouldGetApplication() {
    // Arrange
    final ApplicationEntity entity = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application =
        ApplicationResponse.builder().referenceNumber(entity.getId()).build();
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
