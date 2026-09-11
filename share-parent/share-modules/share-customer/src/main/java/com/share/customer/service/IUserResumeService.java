package com.share.customer.service;

import com.share.customer.domain.interview.UserResume;
import com.share.customer.domain.interview.dto.ResumeAnalysisRequest;
import com.share.customer.domain.interview.dto.ResumeSaveRequest;
import com.share.customer.domain.interview.vo.ResumeAnalysisVO;
import org.springframework.web.multipart.MultipartFile;

public interface IUserResumeService {
    ResumeAnalysisVO getMyResume();

    ResumeAnalysisVO saveResume(ResumeSaveRequest request);

    ResumeAnalysisVO analyzeResume(ResumeAnalysisRequest request);

    String extractResumeText(MultipartFile file);

    void clearResume();

    UserResume getResumeEntity(Long resumeId, Long userId);
}
