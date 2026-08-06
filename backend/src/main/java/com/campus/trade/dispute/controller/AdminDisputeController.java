package com.campus.trade.dispute.controller;

import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.CursorPageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.dispute.dto.HandleDisputeRequest;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import com.campus.trade.dispute.vo.DisputeDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 缂佺媴绱曢幃濠勭博椤栨粎鐪扮紒楣冩敱鐢挳宕ｉ敐蹇曠獥闁哄被鍎冲﹢鍛棯閻樺灚鍊ｉ柛鎺擃殙閵嗗啯绋夋惔銈庢⒖闁告劗鍋ㄩ埀?
 *
 * <p>閺夆晜鐟╅崳閿嬬▔瀹ュ浠橀悷鏇氱閸熸挻绂掔拋宕囩Э闁哄鍟村娲礆閵堝棙鐒介柨娑欘劯@code /admin/**} 鐎规瓕灏欑划锟犳偨?AdminInterceptor 缂備胶鍠嶇粩鎾箯閿旇棄鐒婚柨?
 * 闂傚牏鍋熼鎼佹偠閸℃鍠呴柛锔哄姀缁绘﹢宕?Controller 濞戞柨顑呮晶鐘典焊閸欐绐楅柡鈧捄鍝勭厒 403闁?/p>
 */
@Validated
@RestController
@RequestMapping("/admin/disputes")
public class AdminDisputeController {

    private final DisputeService disputeService;

    public AdminDisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * 婵炴挸鎲￠悥锝夊礆閸℃稏鈧寮婚妷褎绠欑紒鍓уХ閹秹鏁嶇仦钘夎闁圭顦辩猾鍌滅棯妞嬪骸笑闁?0~4 閺夆晛娲﹂幎銈夊Υ?
     *
     * <p>濡絾鐗楅鑲╂嫚闁垮婀村☉鎾崇С缁辫泛銆掗崨濠勫灱闁挎稒绋愮粭鍛▔閳ь剚銇勯棃娑欏€遍柡鍐╂构缁卞爼宕楅妷銈囩憪濞戞挴鍋撳銈夋涧閹奸攱鎯旈弬鎹愬幀闁?cursorCreatedAt 闁?cursorId闁?
     * 濞戞挶鍊撻柌婊呪偓娑欘殕椤斿瞼绱撴潪鎵伇濞戞挸绉磋ぐ鏌ユ晬瀹€鍕級闁稿繐绉撮幃妤冪博椤栨稑鐦诲☉鎾崇Ф鑿欓悗瑙勭濞碱垱绂掗崜浣哄€冲銈囧仯閳?/p>
     */
    @GetMapping
    public Result<CursorPageResult<AdminDisputeVO>> list(
            @RequestParam(required = false) @Min(0) @Max(4) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) @Min(1) Long cursorId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize
    ) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "游标时间和游标 ID 必须同时传入");
        }
        return Result.ok(disputeService.list(status, cursorCreatedAt, cursorId, pageSize));
    }

    /** 閻熶椒绀侀崰鍛棯閻樺灚鍊ｉ柨娑樿嫰閸欐寧鎷呴幘鍐残楀ù锝嗙矎椤?HandleDisputeRequest.action闁?*/
    @PostMapping("/{id}/handle")
    public Result<Void> handle(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody HandleDisputeRequest request
    ) {
        disputeService.handle(id, request);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<DisputeDetailVO> detail(@PathVariable @Min(1) Long id) {
        return Result.ok(disputeService.adminDetail(id));
    }
}
