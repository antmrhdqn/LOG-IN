package com.insider.login.leave.controller;

import com.insider.login.common.response.ResponseMessage;
import com.insider.login.common.utils.TokenUtils;
import com.insider.login.leave.dto.LeaveAccrualDTO;
import com.insider.login.leave.dto.LeaveInfoDTO;
import com.insider.login.leave.dto.LeaveRequestDTO;
import com.insider.login.leave.entity.LeaveAccrualHistory;
import com.insider.login.leave.service.LeaveBalanceService;
import com.insider.login.leave.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveBalanceService leaveBalanceService;

    // ─── 사원용 ───────────────────────────────────────────────────────────────

    @PostMapping("/requests")
    public ResponseEntity<ResponseMessage<Void>> createLeaveRequest(
            @RequestBody LeaveRequestDTO dto) {
        leaveService.createLeaveRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseMessage<>(201, "휴가 신청이 완료되었습니다.", null));
    }

    @GetMapping("/requests")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> selectMyRequests(
            @RequestParam int memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveRequestDTO> result = leaveService.selectMyRequests(memberId, pageable);
        return ResponseEntity.ok(ResponseMessage.success("조회 성공", Map.of("page", result)));
    }

    @PatchMapping("/requests/{id}/cancel")
    public ResponseEntity<ResponseMessage<Void>> cancelRequest(
            @PathVariable("id") Long requestId) {
        leaveService.cancelRequest(requestId);
        return ResponseEntity.ok(ResponseMessage.success("휴가가 취소되었습니다.", null));
    }

    @GetMapping("/balances")
    public ResponseEntity<ResponseMessage<LeaveInfoDTO>> getMyBalances(
            @RequestParam int memberId) {
        LeaveInfoDTO dto = leaveBalanceService.getBalanceInfo(memberId);
        return ResponseEntity.ok(ResponseMessage.success("조회 성공", dto));
    }

    // ─── 결재자용 ─────────────────────────────────────────────────────────────

    @GetMapping("/requests/pending")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> selectPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveRequestDTO> result = leaveService.selectPendingRequests(pageable);
        return ResponseEntity.ok(ResponseMessage.success("조회 성공", Map.of("page", result)));
    }

    @PatchMapping("/requests/{id}/approve")
    public ResponseEntity<ResponseMessage<Void>> approveRequest(
            @PathVariable("id") Long requestId) {
        int approverId = TokenUtils.getTokenInfo().getMemberId();
        leaveService.approveRequest(requestId, approverId);
        return ResponseEntity.ok(ResponseMessage.success("승인 처리되었습니다.", null));
    }

    @PatchMapping("/requests/{id}/reject")
    public ResponseEntity<ResponseMessage<Void>> rejectRequest(
            @PathVariable("id") Long requestId,
            @RequestBody Map<String, String> body) {
        int approverId = TokenUtils.getTokenInfo().getMemberId();
        String rejectReason = body.get("rejectReason");
        leaveService.rejectRequest(requestId, approverId, rejectReason);
        return ResponseEntity.ok(ResponseMessage.success("반려 처리되었습니다.", null));
    }

    // ─── 관리자용 ─────────────────────────────────────────────────────────────

    @PostMapping("/accruals")
    public ResponseEntity<ResponseMessage<Void>> grantSpecialLeave(
            @RequestBody LeaveAccrualDTO dto) {
        int grantorId = TokenUtils.getTokenInfo().getMemberId();
        leaveService.grantSpecialLeave(dto, grantorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseMessage<>(201, "특별휴가가 부여되었습니다.", null));
    }

    @GetMapping("/accruals")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> selectAccrualHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveAccrualHistory> result = leaveService.selectAccrualHistory(pageable);
        return ResponseEntity.ok(ResponseMessage.success("조회 성공", Map.of("page", result)));
    }

    @GetMapping("/balances/all")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> getAllBalances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveInfoDTO> result = leaveBalanceService.getAllBalances(pageable);
        return ResponseEntity.ok(ResponseMessage.success("조회 성공", Map.of("page", result)));
    }
}
