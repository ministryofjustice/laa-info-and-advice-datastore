package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;

/** Unit tests for the {@link RetentionDateUpdateService}. */
@ExtendWith(MockitoExtension.class)
class RetentionDateUpdateServiceTests {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private SystemDrivenEventService systemDrivenEventService;

  private RetentionDateUpdateService sut;

  @BeforeEach
  void setUp() {
    this.sut = new RetentionDateUpdateService(applicationRepository, systemDrivenEventService);
  }

  @Test
  void givenEtagMatches_thenSavesAndRecordsEvent() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.etag(1L));
    final Instant newRetentionDate = Instant.now();
    final UUID claimsId = UUID.randomUUID();
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

    // Act
    sut.saveAndRecord(application.getId(), 1L, null, newRetentionDate, claimsId);

    // Assert
    final ArgumentCaptor<ApplicationEntity> savedCaptor =
        ArgumentCaptor.forClass(ApplicationEntity.class);
    verify(applicationRepository).save(savedCaptor.capture());
    assertThat(savedCaptor.getValue().getDataRetentionDate()).isEqualTo(newRetentionDate);
    verify(systemDrivenEventService)
        .record(
            any(),
            eq(application.getProviderOfficeCode()),
            eq(application.getProviderFirmCode()),
            any(),
            eq(application.getId()));
  }

  @Test
  void givenEtagDoesNotMatch_thenSkipsSaveWithoutThrowing() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.etag(2L));
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

    // Act
    sut.saveAndRecord(application.getId(), 1L, null, Instant.now(), UUID.randomUUID());

    // Assert
    verify(applicationRepository, never()).save(any());
    verify(systemDrivenEventService, never()).record(any(), any(), any(), any(), any());
  }

  @Test
  void givenApplicationNotFound_thenThrowsEntityNotFoundExceptionAndDoesNotSave() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(
            () -> sut.saveAndRecord(applicationId, 1L, null, Instant.now(), UUID.randomUUID()))
        .isInstanceOf(EntityNotFoundException.class);
    verify(applicationRepository, never()).save(any());
    verify(systemDrivenEventService, never()).record(any(), any(), any(), any(), any());
  }
}
