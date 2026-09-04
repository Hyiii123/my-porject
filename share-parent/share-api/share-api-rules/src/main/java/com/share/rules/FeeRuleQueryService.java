package com.share.rules;

import com.share.rules.domain.FeeRule;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 费用规则查询能力的轻量契约。
 *
 * <p>该契约放在 API 模块中，使历史内部接口源码可以保留而不反向依赖
 * share-rule 业务模块，从根上避免 API 与业务模块的循环依赖。</p>
 */
public interface FeeRuleQueryService {
    List<FeeRule> queryByIds(Collection<? extends Serializable> ids);

    FeeRule queryById(Serializable id);
}
