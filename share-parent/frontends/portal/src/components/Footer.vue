<template>
  <footer class="home-footer">
    <div class="home-wrap">
      <div class="home-footer-top">
        <!-- 品牌与愿景 -->
        <div class="home-footer-brand">
          <div class="footer-brand-header">
            <div class="footer-logo-icon">智</div>
            <div class="footer-brand-text">
              <span class="name">智问学伴</span>
              <span class="sub">AI-POWERED COLLABORATIVE EDUCATION</span>
            </div>
          </div>
          <p class="brand-desc">
            智问学伴融合前沿大模型博弈论导学算法与布鲁姆认知阶梯模型，致力于打破传统在线教育“千人一面”与“半途劝退”困局，为每一位学习者打造自适应闭环成长路径。
          </p>
        </div>

        <!-- 组织机构与产学研生态 -->
        <div class="home-fcol home-fcol-org">
          <h4>产学研协同矩阵</h4>
          <p><span class="lab">学术指导：</span>国家人工智能产教融合创新平台 / 计算机教学指导委员会</p>
          <p><span class="lab">核心引擎：</span>L5 级自省智能体集群 · Kahn DAG 拓扑质检引擎</p>
          <p><span class="lab">课程标准：</span>国家 IT 软件工程师胜任力图谱 & 大厂 P6/P7 架构标准</p>
          <p><span class="lab">支撑单位：</span>智问人工智能教育研究院 / 产学研协同创新基地</p>
        </div>

        <!-- 平台咨询与技术支持 -->
        <div class="home-fcol home-fcol-contact">
          <h4>咨询与技术交流</h4>
          <p><span class="lab">客服热线：</span><span class="mono">400-880-2026</span></p>
          <p><span class="lab">技术支持：</span><span class="mono">tech-support@zhiwen-ai.cn</span></p>
          <p><span class="lab">服务时间：</span>周一至周日 09:00 - 22:00 全天候在线响应</p>
          <p><span class="lab">官方社区：</span>智问学伴技术进阶交流圈（加入学习群领取 2026 架构大纲）</p>
        </div>
      </div>

      <!-- 底部版权与备案 -->
      <div class="home-footer-bottom">
        <div class="copyright">
          © 2026 智问学伴在线教育系统 · 保留所有权利 (Powered by Multi-Agent Graph Engine & DeepSeek)
        </div>
        <div class="filing">
          <span>渝 ICP 备 20260918 号 - 1</span>
          <span class="sep">|</span>
          <span>高校产学研协同教育示范工程</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.home-footer {
  position: relative;
  overflow: hidden;
  background: #05070C;
  color: #94A3B8;
  padding: 64px 0 32px;
  font-family: var(--cn);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 20%;
    width: 60%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(56, 189, 248, 0.4), transparent);
    pointer-events: none;
  }
}

.home-wrap {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 28px;
}

.home-footer-top {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.2fr 1.3fr 1.1fr;
  align-items: start;
  gap: 48px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  padding-bottom: 40px;
}

.home-footer-brand {
  .footer-brand-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }
  .footer-logo-icon {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: linear-gradient(135deg, rgba(56, 189, 248, 0.2) 0%, rgba(99, 102, 241, 0.2) 100%);
    border: 1px solid rgba(56, 189, 248, 0.45);
    color: #38BDF8;
    font-family: var(--display);
    font-weight: 900;
    font-size: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 0 16px -2px rgba(56, 189, 248, 0.3);
  }
  .footer-brand-text {
    display: flex;
    flex-direction: column;
    .name {
      font-family: var(--display);
      font-weight: 800;
      font-size: 19px;
      letter-spacing: -0.02em;
      color: #FFFFFF;
      line-height: 1.15;
    }
    .sub {
      font-family: var(--mono);
      font-size: 9.5px;
      color: #38BDF8;
      letter-spacing: 0.12em;
      margin-top: 3px;
      font-weight: 600;
    }
  }
  .brand-desc {
    font-size: 13.5px;
    line-height: 1.7;
    color: #94A3B8;
    margin: 0;
  }
}

.home-fcol {
  h4 {
    font-size: 15px;
    font-weight: 600;
    color: #FFFFFF;
    margin-bottom: 18px;
    letter-spacing: -0.01em;
    position: relative;
    padding-left: 10px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 3px;
      bottom: 3px;
      width: 3px;
      border-radius: 2px;
      background: linear-gradient(180deg, #38BDF8, #818CF8);
    }
  }
  p {
    font-size: 13px;
    line-height: 1.8;
    color: #94A3B8;
    margin: 0 0 8px 0;
    display: flex;
    align-items: baseline;

    .lab {
      color: #CBD5E1;
      font-weight: 500;
      flex-shrink: 0;
    }
    .mono {
      font-family: var(--mono);
      color: #38BDF8;
      font-weight: 500;
    }
  }
}

.home-footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 24px;
  font-size: 12.5px;
  color: #64748B;
  font-family: var(--mono);

  .copyright {
    letter-spacing: 0.02em;
  }

  .filing {
    display: flex;
    align-items: center;
    gap: 8px;
    .sep {
      color: rgba(255, 255, 255, 0.15);
    }
  }
}

@media (max-width: 900px) {
  .home-footer-top {
    grid-template-columns: 1fr;
    gap: 32px;
  }
  .home-footer-bottom {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}
</style>