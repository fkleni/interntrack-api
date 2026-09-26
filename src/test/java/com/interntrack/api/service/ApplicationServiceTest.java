package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.User;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private ApplicationService applicationService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("fadime");
        currentUser.setEmail("fadime@example.com");

        // Simulate an authenticated request context, the way Spring Security would set it up
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("fadime", null)
        );

        when(userRepository.findByUsername("fadime")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Application sampleApplication(Long id) {
        Application app = new Application();
        app.setId(id);
        app.setCompanyName("Google");
        app.setPosition("Backend Intern");
        app.setStatus("Applied");
        app.setAppliedDate(LocalDate.now());
        app.setOwner(currentUser);
        return app;
    }

    // --- Ownership isolation ---

    @Test
    void getApplicationById_returnsApplication_whenOwnedByCurrentUser() {
        Application app = sampleApplication(5L);
        when(applicationRepository.findByIdAndOwner(5L, currentUser)).thenReturn(Optional.of(app));

        Application result = applicationService.getApplicationById(5L);

        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void getApplicationById_throwsNotFound_whenApplicationBelongsToAnotherUser() {
        // findByIdAndOwner scopes the query to the current user, so someone else's
        // application simply won't be found — this simulates that outcome.
        when(applicationRepository.findByIdAndOwner(99L, currentUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- jobDescription change invalidates aiInsight ---

    @Test
    void updateApplication_clearsAiInsight_whenJobDescriptionChanges() {
        Application existing = sampleApplication(1L);
        existing.setJobDescription("Old description");
        existing.setAiInsight("Some previous analysis");
        existing.setAiInsightGeneratedAt(LocalDateTime.now());

        when(applicationRepository.findByIdAndOwner(1L, currentUser)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application updates = sampleApplication(1L);
        updates.setJobDescription("New, different description");

        Application result = applicationService.updateApplication(1L, updates);

        assertThat(result.getAiInsight()).isNull();
        assertThat(result.getAiInsightGeneratedAt()).isNull();
    }

    @Test
    void updateApplication_keepsAiInsight_whenJobDescriptionUnchanged() {
        Application existing = sampleApplication(1L);
        existing.setJobDescription("Same description");
        existing.setAiInsight("Some previous analysis");
        existing.setAiInsightGeneratedAt(LocalDateTime.now());

        when(applicationRepository.findByIdAndOwner(1L, currentUser)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application updates = sampleApplication(1L);
        updates.setJobDescription("Same description");

        Application result = applicationService.updateApplication(1L, updates);

        assertThat(result.getAiInsight()).isEqualTo("Some previous analysis");
    }

    // --- CV upload/delete invalidates aiInsight ---

    @Test
    void deleteCv_clearsAiInsight() {
        Application existing = sampleApplication(1L);
        existing.setCvFilePath("https://res.cloudinary.com/example.pdf");
        existing.setAiInsight("Some previous analysis");
        existing.setAiInsightGeneratedAt(LocalDateTime.now());

        when(applicationRepository.findByIdAndOwner(1L, currentUser)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.deleteCv(1L);

        assertThat(existing.getAiInsight()).isNull();
        assertThat(existing.getCvFilePath()).isNull();
        verify(fileStorageService).delete("https://res.cloudinary.com/example.pdf");
    }

    // --- analyzeApplication: cache short-circuit ---

    @Test
    void analyzeApplication_returnsCachedInsight_withoutCallingGemini() {
        Application existing = sampleApplication(1L);
        existing.setJobDescription("Some job description");
        existing.setCvFilePath("https://res.cloudinary.com/example.pdf");
        existing.setAiInsight("Already analyzed");

        when(applicationRepository.findByIdAndOwner(1L, currentUser)).thenReturn(Optional.of(existing));

        Application result = applicationService.analyzeApplication(1L);

        assertThat(result.getAiInsight()).isEqualTo("Already analyzed");
        verify(geminiService, never()).analyzeJobFit(any(), any());
    }

    @Test
    void analyzeApplication_callsGemini_whenNoCachedInsightExists() {
        Application existing = sampleApplication(1L);
        existing.setJobDescription("Some job description");
        existing.setCvFilePath("https://res.cloudinary.com/example.pdf");
        existing.setAiInsight(null);

        when(applicationRepository.findByIdAndOwner(1L, currentUser)).thenReturn(Optional.of(existing));
        when(fileStorageService.downloadBytes(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(geminiService.analyzeJobFit(anyString(), any(byte[].class))).thenReturn("Fresh analysis result");
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.analyzeApplication(1L);

        assertThat(result.getAiInsight()).isEqualTo("Fresh analysis result");
        assertThat(result.getAiInsightGeneratedAt()).isNotNull();
        verify(geminiService).analyzeJobFit(anyString(), any(byte[].class));
    }

    @Test
    void analyzeApplication_throwsIllegalArgument_whenJobDescriptionMissing() {
        Application existing = sampleApplication(1L);
        existing.setJobDescription(null);
        existing.setCvFilePath("https://res.cloudinary.com/example.pdf");

        when(applicationRepository.findByIdAndOwner(1L, currentUser)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.analyzeApplication(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job description");
    }

    @Test
    void analyzeApplication_throwsIllegalArgument_whenCvMissing() {
        Application existing = sampleApplication(1L);
        existing.setJobDescription("Some job description");
        existing.setCvFilePath(null);

        when(applicationRepository.findByIdAndOwner(1L, currentUser)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.analyzeApplication(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CV");
    }
}