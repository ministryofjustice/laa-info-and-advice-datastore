package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;

/** Unit tests for the {@link DraftDataDeletionTask}. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
class DraftDataDeletionTaskTests {

  private static final int DRAFT_RETENTION_YEARS_OFFSET = 2;
  private static final String SOURCE = DraftDataDeletionTask.class.getSimpleName();

  @Mock private ApplicationRepository applicationRepository;
  @Mock private ApplicationDataDeletionService applicationDataDeletionService;

  private DraftDataDeletionTask sut;

  @BeforeEach
  void setUp() {
    this.sut =
        new DraftDataDeletionTask(
            applicationRepository, DRAFT_RETENTION_YEARS_OFFSET, applicationDataDeletionService);
  }

  @SuppressWarnings("unchecked")
  private static Specification<ApplicationEntity> anySpecification() {
    return any(Specification.class);
  }

  @Test
  void givenNoExpiredDrafts_thenDoNothing() {
    // Arrange
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of());

    // Act
    sut.run();

    // Assert
    verify(applicationDataDeletionService, never()).deleteAndRecord(any(), anyLong(), any());
  }

  @Test
  void givenExpiredDraft_thenDeleteApplicationWithDraftTaskAsSource() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.withDefaultClientDetails());
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));

    // Act
    sut.run();

    // Assert
    verify(applicationDataDeletionService, times(1))
        .deleteAndRecord(application.getId(), application.getEtag(), SOURCE);
  }

  @Test
  @SuppressWarnings("unchecked")
  void whenRun_thenQueriesForDraftsNotModifiedForTheConfiguredNumberOfYears() {
    // Arrange
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of());
    final Root<ApplicationEntity> root = mock(Root.class);
    final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    when(root.get(any(String.class))).thenReturn(mock(Path.class));

    // Act
    sut.run();

    // Assert
    final ArgumentCaptor<Specification<ApplicationEntity>> specCaptor =
        ArgumentCaptor.forClass(Specification.class);
    verify(applicationRepository).findAll(specCaptor.capture());
    specCaptor.getValue().toPredicate(root, mock(CriteriaQuery.class), criteriaBuilder);
    final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(criteriaBuilder).lessThan(any(Path.class), cutoffCaptor.capture());
    final Instant expectedCutoff =
        Instant.now().minus(DRAFT_RETENTION_YEARS_OFFSET * 365L, ChronoUnit.DAYS);
    assertThat(Duration.between(expectedCutoff, cutoffCaptor.getValue()).abs())
        .isLessThan(Duration.ofDays(2));
  }

  @Test
  void givenOneApplicationThrows_thenContinueProcessingOthers() {
    // Arrange
    final ApplicationEntity failingApplication =
        ApplicationEntityGenerator.createWithId(builder -> builder.withDefaultClientDetails());
    final ApplicationEntity succeedingApplication =
        ApplicationEntityGenerator.createWithId(builder -> builder.withDefaultClientDetails());
    when(applicationRepository.findAll(anySpecification()))
        .thenReturn(List.of(failingApplication, succeedingApplication));
    doThrow(new RuntimeException("DB constraint violation"))
        .when(applicationDataDeletionService)
        .deleteAndRecord(eq(failingApplication.getId()), anyLong(), any());

    // Act
    sut.run();

    // Assert
    verify(applicationDataDeletionService, times(1))
        .deleteAndRecord(failingApplication.getId(), failingApplication.getEtag(), SOURCE);
    verify(applicationDataDeletionService, times(1))
        .deleteAndRecord(succeedingApplication.getId(), succeedingApplication.getEtag(), SOURCE);
  }
}
