package com.insider.login.leave.controller;

import com.insider.login.common.CommonController;
import com.insider.login.common.response.ResponseMessage;
import com.insider.login.leave.dto.LeaveAccrualDTO;
import com.insider.login.leave.dto.LeaveInfoDTO;
import com.insider.login.leave.dto.LeaveMemberDTO;
import com.insider.login.leave.dto.LeaveSubmitDTO;
import com.insider.login.leave.service.LeaveService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.insider.login.common.utils.TokenUtils.getTokenInfo;

@RestController
@Slf4j
public class LeaveController extends CommonController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * 휴가 신청 내역 조회
     */
    @GetMapping("/leaveSubmits")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> selectSubmitList(
            @RequestParam(value = "page", defaultValue = "0") int pageNumber,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction,
            @RequestParam(value = "properties", defaultValue = "leaveSubNo") String properties,
            @RequestParam(value = "memberId", defaultValue = "0") int memberId) {

        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(properties).ascending() : Sort.by(properties).descending();
        Pageable pageable = PageRequest.of(pageNumber, 10, sort);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("page", leaveService.selectLeaveSubmitList(memberId, pageable));
        if (memberId != 0) {
            responseMap.put("leaveInfo", leaveService.getLeaveInfoById(memberId));
        }

        return ResponseEntity.ok(ResponseMessage.success("조회 성공", responseMap));
    }

    /**
     * 휴가 신청
     */
    @PostMapping("/leaveSubmits")
    public ResponseEntity<ResponseMessage<Void>> insertSubmit(@RequestBody LeaveSubmitDTO leaveSubmitDTO) {
        leaveSubmitDTO.setLeaveSubApplyDate(nowDate());
        leaveService.insertSubmit(leaveSubmitDTO);

        return ResponseEntity.ok(ResponseMessage.success("신청 등록 성공", null));
    }

    /**
     * 휴가 신청 취소 (삭제)
     */
    @DeleteMapping("/leaveSubmits/{LeaveSubNo}")
    public ResponseEntity<ResponseMessage<Void>> deleteSubmit(@PathVariable("LeaveSubNo") int leaveSubNo) {
        leaveService.deleteSubmit(leaveSubNo);

        return ResponseEntity.ok(ResponseMessage.success("신청 취소 성공", null));
    }

    /**
     * 발생 내역 조회
     */
    @GetMapping("/leaveAccruals")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> selectAccrualList(
            @RequestParam(value = "page", defaultValue = "0") int pageNumber,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction,
            @RequestParam(value = "properties", defaultValue = "leaveAccrualNo") String properties) {

        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(properties).ascending() : Sort.by(properties).descending();
        Pageable pageable = PageRequest.of(pageNumber, 10, sort);

        Page<LeaveAccrualDTO> page = leaveService.selectAccrualList(pageable);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("page", page);

        return ResponseEntity.ok(ResponseMessage.success("조회 성공", responseMap));
    }

    /**
     * 휴가 발생 등록
     */
    @PostMapping("/leaveAccruals")
    public ResponseEntity<ResponseMessage<Void>> insertAccrual(@RequestBody LeaveAccrualDTO leaveAccrualDTO) {
        leaveAccrualDTO.setAccrualDate(nowDate());
        leaveService.insertAccrual(leaveAccrualDTO);

        return ResponseEntity.ok(ResponseMessage.success("휴가발생 등록 성공", null));
    }

    /**
     * 사원 검색 (이름 기준)
     */
    @GetMapping("/leaveAccruals/{name}")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> selectMemberList(@PathVariable("name") String name) {
        List<LeaveMemberDTO> memberList = leaveService.selectMemberList(name);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("memberList", memberList);

        return ResponseEntity.ok(ResponseMessage.success("조회 성공", responseMap));
    }

    /**
     * 휴가 신청 처리 (승인/반려 등)
     */
    @PutMapping("/leaveSubmits")
    public ResponseEntity<ResponseMessage<Void>> updateSubmit(@RequestBody LeaveSubmitDTO leaveSubmitDTO) {
        leaveSubmitDTO.setLeaveSubApprover(getTokenInfo().getMemberId());
        leaveSubmitDTO.setLeaveSubProcessDate(nowDate());

        leaveService.updateSubmit(leaveSubmitDTO);

        return ResponseEntity.ok(ResponseMessage.success("휴가 처리 성공", null));
    }

    /**
     * 휴가 보유 내역 조회
     */
    @GetMapping("/leaves")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> selectLeavesList(
            @RequestParam(value = "page", defaultValue = "0") int pageNumber,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction,
            @RequestParam(value = "properties", defaultValue = "leaveSubNo") String properties) {

        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(properties).ascending() : Sort.by(properties).descending();
        Pageable pageable = PageRequest.of(pageNumber, 10, sort);

        Page<LeaveInfoDTO> page = leaveService.selectLeavesList(pageable);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("page", page);

        return ResponseEntity.ok(ResponseMessage.success("조회 성공", responseMap));
    }
}