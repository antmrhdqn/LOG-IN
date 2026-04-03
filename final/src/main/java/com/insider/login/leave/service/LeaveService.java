package com.insider.login.leave.service;

import com.insider.login.calendar.dto.CalendarDTO;
import com.insider.login.calendar.entity.Calendar;
import com.insider.login.calendar.repository.CalendarRepository;
import com.insider.login.common.error.ErrorCode;
import com.insider.login.common.error.exception.BusinessException;
import com.insider.login.department.entity.Department;
import com.insider.login.department.repository.DepartmentRepository;
import com.insider.login.leave.dto.*;
import com.insider.login.leave.entity.*;
import com.insider.login.leave.repository.*;
import com.insider.login.leave.util.LeaveUtil;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.insider.login.common.CommonController.nowDate;
import static com.insider.login.common.utils.TokenUtils.getTokenInfo;

@Service
@Slf4j
public class LeaveService extends LeaveUtil {

    private final LeaveAccrualRepository leaveAccrualRepository;
    private final LeaveRepository leaveRepository;
    private final LeaveSubmitRepository leaveSubmitRepository;
    private final ModelMapper modelMapper;
    private final LeaveMemberRepository memberRepository;
    private final DepartmentRepository departmentRepository;
    private final LeavePositionRepository positionRepository;
    private final CalendarRepository calendarRepository;
    private final SubmitAndCalendarRepository submitAndCalendarRepository;

    public LeaveService(LeaveAccrualRepository leaveAccrualRepository, LeaveRepository leaveRepository, LeaveSubmitRepository leaveSubmitRepository, ModelMapper modelMapper, LeaveMemberRepository memberRepository, DepartmentRepository departmentRepository, LeavePositionRepository positionRepository, CalendarRepository calendarRepository, SubmitAndCalendarRepository submitAndCalendarRepository) {
        this.leaveAccrualRepository = leaveAccrualRepository;
        this.leaveRepository = leaveRepository;
        this.leaveSubmitRepository = leaveSubmitRepository;
        this.modelMapper = modelMapper;
        this.memberRepository = memberRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.calendarRepository = calendarRepository;
        this.submitAndCalendarRepository = submitAndCalendarRepository;
    }

    public Page<LeaveSubmitDTO> selectLeaveSubmitList(int applicantId, Pageable pageable) {
        Page<LeaveSubmit> submitList = (applicantId > 0)
                ? leaveSubmitRepository.findByMemberId(applicantId, pageable)
                : leaveSubmitRepository.findAll(pageable);

        List<LeaveSubmitDTO> DTOList = submitList.stream().map(submit -> {
            LeaveSubmitDTO dto = modelMapper.map(submit, LeaveSubmitDTO.class);
            dto.setApplicantName(memberRepository.findNameByMemberId(submit.getLeaveSubApplicant()));
            if (submit.getLeaveSubApprover() != 0) {
                dto.setApproverName(memberRepository.findNameByMemberId(submit.getLeaveSubApprover()));
            }
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(DTOList, submitList.getPageable(), submitList.getTotalElements());
    }

    @Transactional
    public void insertSubmit(LeaveSubmitDTO DTO) {
        if (DTO.getLeaveSubNo() != 0) {
            int originalNo = DTO.getLeaveSubNo();
            LeaveSubmit originalSubmit = leaveSubmitRepository.findById(originalNo)
                    .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_NOT_FOUND));

            // 엔티티 직접 수정 대신 DTO 변환 후 처리 (Setter 문제 방지)
            LeaveSubmitDTO updateDTO = modelMapper.map(originalSubmit, LeaveSubmitDTO.class);
            updateDTO.setLeaveSubStatus("취소신청");
            leaveSubmitRepository.save(modelMapper.map(updateDTO, LeaveSubmit.class));

            DTO.setRefLeaveSubNo(originalNo);
            DTO.setLeaveSubNo(0);
        }

        DTO.setLeaveSubStatus("대기");
        leaveSubmitRepository.save(modelMapper.map(DTO, LeaveSubmit.class));
    }

    public Page<LeaveAccrualDTO> selectAccrualList(Pageable pageable) {
        Page<LeaveAccrual> accrualList = leaveAccrualRepository.findAll(pageable);
        List<LeaveAccrualDTO> DTOList = accrualList.stream().map(accrual -> {
            LeaveAccrualDTO dto = modelMapper.map(accrual, LeaveAccrualDTO.class);
            dto.setRecipientName(memberRepository.findNameByMemberId(accrual.getRecipientId()));
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(DTOList, accrualList.getPageable(), accrualList.getTotalElements());
    }

    @Transactional
    public void insertAccrual(LeaveAccrualDTO accrualDTO) {
        LeaveAccrual ett = leaveAccrualRepository.save(modelMapper.map(accrualDTO, LeaveAccrual.class));
        leaveRepository.save(new Leaves(ett.getRecipientId(), ett.getLeaveAccrualDays(), "특별휴가"));

        LeaveSubmitDTO submitDTO = new LeaveSubmitDTO();
        submitDTO.setLeaveSubApplicant(accrualDTO.getRecipientId());
        submitDTO.setLeaveSubApprover(getTokenInfo().getMemberId());
        submitDTO.setLeaveSubStartDate(accrualDTO.getLeaveSubStartDate());
        submitDTO.setLeaveSubEndDate(accrualDTO.getLeaveSubEndDate());
        submitDTO.setLeaveSubApplyDate(nowDate());
        submitDTO.setLeaveSubType("특별휴가");
        submitDTO.setLeaveSubStatus("발생");
        submitDTO.setLeaveSubProcessDate(nowDate());
        submitDTO.setLeaveSubReason(accrualDTO.getLeaveAccrualReason());

        LeaveSubmit submit = leaveSubmitRepository.save(modelMapper.map(submitDTO, LeaveSubmit.class));
        Calendar calendar = modelMapper.map(submitCalendar(submit), Calendar.class);
        calendarRepository.save(calendar);
        submitAndCalendarRepository.save(new SubmitAndCalendar(submit.getLeaveSubNo(), calendar.getCalendarNo()));
    }

    public List<LeaveMemberDTO> selectMemberList(String name) {
        return memberRepository.findByName(name).stream()
                .map(member -> {
                    LeaveMemberDTO dto = modelMapper.map(member, LeaveMemberDTO.class);
                    dto.setDepartment(getDepartment(dto.getDepartNo()));
                    return dto;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteSubmit(int leaveSubNo) {
        leaveSubmitRepository.findById(leaveSubNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_NOT_FOUND));
        leaveSubmitRepository.deleteById(leaveSubNo);
    }

    @Transactional
    public void updateSubmit(LeaveSubmitDTO leaveSubmitDTO) {
        LeaveSubmit currentEntity = leaveSubmitRepository.findById(leaveSubmitDTO.getLeaveSubNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_NOT_FOUND));

        LeaveSubmitDTO newSubmit = modelMapper.map(currentEntity, LeaveSubmitDTO.class);
        newSubmit.setLeaveSubApprover(leaveSubmitDTO.getLeaveSubApprover());
        newSubmit.setLeaveSubStatus(leaveSubmitDTO.getLeaveSubStatus());
        newSubmit.setLeaveSubProcessDate(leaveSubmitDTO.getLeaveSubProcessDate());

        if ("반려".equals(leaveSubmitDTO.getLeaveSubStatus())) {
            newSubmit.setLeaveSubReason(leaveSubmitDTO.getLeaveSubReason());
        }

        if (newSubmit.getRefLeaveSubNo() != 0) {
            LeaveSubmit refEntity = leaveSubmitRepository.findById(newSubmit.getRefLeaveSubNo())
                    .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_NOT_FOUND));
            LeaveSubmitDTO refSubmit = modelMapper.map(refEntity, LeaveSubmitDTO.class);

            if ("승인".equals(leaveSubmitDTO.getLeaveSubStatus())) {
                refSubmit.setLeaveSubStatus("취소승인");
                SubmitAndCalendar relation = submitAndCalendarRepository.findByLeaveSubNo(newSubmit.getRefLeaveSubNo());
                if (relation != null) calendarRepository.deleteById(relation.getCalendarNo());
            } else {
                refSubmit.setLeaveSubStatus("취소반려");
                refSubmit.setLeaveSubReason(leaveSubmitDTO.getLeaveSubReason());
            }
            leaveSubmitRepository.save(modelMapper.map(refSubmit, LeaveSubmit.class));
        } else if ("승인".equals(newSubmit.getLeaveSubStatus())) {
            Calendar calendar = modelMapper.map(submitCalendar(modelMapper.map(newSubmit, LeaveSubmit.class)), Calendar.class);
            calendarRepository.save(calendar);
            submitAndCalendarRepository.save(new SubmitAndCalendar(newSubmit.getLeaveSubNo(), calendar.getCalendarNo()));
        }
        leaveSubmitRepository.save(modelMapper.map(newSubmit, LeaveSubmit.class));
    }

    public Page<LeaveInfoDTO> selectLeavesList(Pageable pageable) {
        List<LeaveMember> memberList = memberRepository.findAll();
        List<LeaveInfoDTO> infoDTOList = memberList.stream()
                .map(member -> {
                    LeaveInfoDTO infoDTO = getLeaveInfoById(member.getMemberId());
                    return (infoDTO != null) ? infoDTO : new LeaveInfoDTO(member.getMemberId(), member.getName(), 0, 0, 0, 0, 0);
                }).collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), memberList.size());
        return new PageImpl<>(infoDTOList.subList(start, end), pageable, infoDTOList.size());
    }

    // --- 헬퍼 메서드 (복구) ---

    public LeaveInfoDTO getLeaveInfoById(int memberId) {
        List<Leaves> leavesList = leaveRepository.findByMemberId(memberId);
        if (leavesList.isEmpty()) return null;

        LeaveInfoDTO info = leaveInfoCalc(leavesList);
        info.setName(memberRepository.findNameByMemberId(memberId));
        return addLeaveInfo(info);
    }

    public LeaveInfoDTO addLeaveInfo(LeaveInfoDTO DTO) {
        List<LeaveSubmit> submitList = leaveSubmitRepository.findByMemberId(DTO.getMemberId());
        int consumedDays = submitList.stream()
                .filter(s -> !"취소".equals(s.getLeaveSubType()) && !"취소".equals(s.getLeaveSubStatus()) &&
                        !"반려".equals(s.getLeaveSubStatus()) && !"취소승인".equals(s.getLeaveSubStatus()))
                .mapToInt(this::leaveDaysCalc)
                .sum();

        DTO.setConsumedDays(consumedDays);
        DTO.setRemainingDays(DTO.getTotalDays() - consumedDays);
        return DTO;
    }

    public String getDepartment(int departNo) {
        return departmentRepository.findById(departNo)
                .map(Department::getDepartName)
                .orElse("없음");
    }

    public CalendarDTO submitCalendar(LeaveSubmit updatedSubmit) {
        Map<String, String> memberInfo = getMemberInfo(updatedSubmit.getLeaveSubApplicant());
        Map<String, LocalDateTime> calendarDateTime = getCalendarDateTime(updatedSubmit);

        CalendarDTO calendarDTO = new CalendarDTO();
        calendarDTO.setCalendarName(memberInfo.get("department") + " " + memberInfo.get("name") + " " + memberInfo.get("position") + " 휴가");
        calendarDTO.setCalendarStart(calendarDateTime.get("start"));
        calendarDTO.setCalendarEnd(calendarDateTime.get("end"));
        calendarDTO.setColor("yellow");
        calendarDTO.setDepartment(memberInfo.get("department"));
        calendarDTO.setRegistrantId(getTokenInfo().getMemberId());
        calendarDTO.setDetail(updatedSubmit.getLeaveSubType());

        return calendarDTO;
    }

    public Map<String, String> getMemberInfo(int memberId) {
        Map<String, String> map = new HashMap<>();
        map.put("name", memberRepository.findNameByMemberId(memberId));
        map.put("department", getDepartment(memberRepository.findDepartNoByMemberId(memberId)));
        String posLevel = memberRepository.findPositionLevelByMemberId(memberId);
        map.put("position", positionRepository.findPositionNameByPositionLevel(posLevel));
        return map;
    }

    public Map<String, LocalDateTime> getCalendarDateTime(LeaveSubmit updatedSubmit) {
        Map<String, LocalDateTime> map = new HashMap<>();
        LocalDate start = updatedSubmit.getLeaveSubStartDate();
        LocalDate end = updatedSubmit.getLeaveSubEndDate();
        map.put("start", "오후반차".equals(updatedSubmit.getLeaveSubType()) ? start.atTime(14, 0) : start.atTime(9, 0));
        map.put("end", "오전반차".equals(updatedSubmit.getLeaveSubType()) ? end.atTime(13, 0) : end.atTime(18, 0));
        return map;
    }
}