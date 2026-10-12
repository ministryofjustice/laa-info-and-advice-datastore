package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

/** Unit tests for the {@link ApplicationDataDeletionService}. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
class ApplicationDataDeletionServiceTests {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private EventRepository eventRepository;
  @Mock private SystemDrivenEventService systemDrivenEventService;

  private ApplicationDataDeletionService sut;

  @BeforeEach
  void setUp() {
    this.sut =
        new ApplicationDataDeletionService(
            applicationRepository, eventRepository, systemDrivenEventService);
  }

  @Test
  void givenEtagMatches_thenNullsPiiMarksDeletedAndRecordsEvent() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().etag(1L));
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
    final EventEntity existingEvent =
        EventEntity.builder()
            .applicationId(application.getId())
            .piiData(JsonNodeFactory.instance.objectNode().put("some-uuid", "Jane"))
            .build();
    when(eventRepository.findByApplicationId(application.getId()))
        .thenReturn(List.of(existingEvent));

    // Act
    sut.deleteAndRecord(application.getId(), 1L);

    // Assert
    final ArgumentCaptor<ApplicationEntity> savedCaptor =
        ArgumentCaptor.forClass(ApplicationEntity.class);
    verify(applicationRepository).save(savedCaptor.capture());
    final ApplicationEntity saved = savedCaptor.getValue();
    assertThat(saved.isDeleted()).isTrue();
    assertThat(saved.getDeletedAt()).isNotNull();
    assertThat(saved.getClientDetails().getFirstName()).isNull();
    assertThat(saved.getClientDetails().getLastName()).isNull();
    assertThat(saved.getClientDetails().getDateOfBirth()).isNull();
    assertThat(saved.getClientDetails().getNiNumber()).isNull();
    assertThat(saved.getClientDetails().getAddress().getAddressLine1()).isNull();
    assertThat(saved.getClientDetails().getAddress().getAddressLine2()).isNull();
    assertThat(saved.getClientDetails().getAddress().getTownOrCity()).isNull();
    assertThat(saved.getClientDetails().getAddress().getPostCode()).isNull();

    verify(eventRepository).save(existingEvent);
    assertThat(existingEvent.getPiiData()).isNull();

    verify(systemDrivenEventService)
        .record(
            any(),
            eq(application.getProviderOfficeCode()),
            eq(application.getProviderFirmCode()),
            any(),
            eq(application.getId()));
  }

  @Test
  void givenEtagDoesNotMatch_thenSkipsWithoutThrowing() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().etag(2L));
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

    // Act
    sut.deleteAndRecord(application.getId(), 1L);

    // Assert
    verify(applicationRepository, never()).save(any());
    verify(eventRepository, never()).findByApplicationId(any());
    verify(systemDrivenEventService, never()).record(any(), any(), any(), any(), any());
  }

  @Test
  void givenApplicationNotFound_thenThrowsEntityNotFoundExceptionAndDoesNotSave() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> sut.deleteAndRecord(applicationId, 1L))
        .isInstanceOf(EntityNotFoundException.class);
    verify(applicationRepository, never()).save(any());
    verify(systemDrivenEventService, never()).record(any(), any(), any(), any(), any());
  }

  @Test
  void givenClientDetailsHasNoAddress_thenOnlyClientDetailsFieldsAreNulled() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .clientDetails(
                        ClientDetailsEntityGenerator.createWithId(
                            clientBuilder -> clientBuilder.address(null)))
                    .etag(1L));
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
    when(eventRepository.findByApplicationId(application.getId())).thenReturn(List.of());

    // Act
    sut.deleteAndRecord(application.getId(), 1L);

    // Assert
    final ArgumentCaptor<ApplicationEntity> savedCaptor =
        ArgumentCaptor.forClass(ApplicationEntity.class);
    verify(applicationRepository).save(savedCaptor.capture());
    assertThat(savedCaptor.getValue().getClientDetails().getFirstName()).isNull();
    assertThat(savedCaptor.getValue().getClientDetails().getAddress()).isNull();
    verify(eventRepository, never()).save(any());
  }

  @Test
  void givenNoExistingEventsForApplication_thenDoesNotAttemptToSaveAnyEvent() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .clientDetails(
                        ClientDetailsEntityGenerator.createWithId(
                            clientBuilder ->
                                clientBuilder.address(AddressEntityGenerator.createWithId(null))))
                    .etag(1L));
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
    when(eventRepository.findByApplicationId(application.getId())).thenReturn(List.of());

    // Act
    sut.deleteAndRecord(application.getId(), 1L);

    // Assert
    verify(eventRepository, never()).save(any());
    verify(applicationRepository, times(1)).save(any());
  }
}
