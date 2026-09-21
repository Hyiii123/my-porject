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
  background: var(--navy-2);
  color: #c6d6ee;
  padding: 56px 0 28px;
  font-family: var(--cn);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.home-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 28px;
}

.home-footer-top {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.1fr 1.4fr 1.2fr;
  align-items: start;
  gap: 36px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 32px;
}

.home-footer-brand {
  .footer-brand-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 14px;
  }
  .footer-logo-icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    background: var(--grad);
    color: #fff;
    font-family: var(--display);
    font-weight: 900;
    font-size: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(33, 198, 232, 0.35);
  }
  .footer-brand-text {
    display: flex;
    flex-direction: column;
    .name {
      font-family: var(--display);
      font-weight: 800;
      font-size: 18px;
      letter-spacing: 0.05em;
      color: #fff;
      line-height: 1.1;
    }
    .sub {
      font-family: var(--mono);
      font-size: 10px;
      color: #c6d6ee99;
      letter-spacing: 0.08em;
      margin-top: 3px;
    }
  }
  .brand-desc {
    font-size: 13.5px;
    color: #a3b8d6;
    line-height: 1.7;
    margin: 0;
  }
}

.home-fcol {
  h4 {
    font-size: 14px;
    letter-spacing: 0.06em;
    color: var(--cyan);
    margin-bottom: 14px;
    font-weight: 700;
    text-transform: uppercase;
  }
  p {
    font-size: 13px;
    color: #dce8f8;
    margin-bottom: 8px;
    line-height: 1.7;
    .lab {
      color: #8fa5c4;
      margin-right: 6px;
    }
  }
  .mono {
    font-family: var(--mono);
    color: #fff;
    font-weight: 600;
  }
}

.home-footer-bottom {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  padding-top: 20px;
  font-size: 12.5px;
  color: #c6d6ee80;

  .sep {
    margin: 0 8px;
    opacity: 0.4;
  }
}

@media (max-width: 900px) {
  .home-footer-top {
    grid-template-columns: 1fr;
    gap: 28px;
  }
}
</style>
