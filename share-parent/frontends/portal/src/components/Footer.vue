<!-- 公用底部组件 -->
<template>
  <footer>
    <div class="container">
      <div class="links">
        <span v-for="item in Links" :key="item.title" @click="goDetails(item.link)">{{ item.title }}</span>
      </div>
      <div class="about fx-sb">
        <div class="info">
          <p>智问学伴致力于普及优质在线教育，融汇名师讲堂、互动实验与智能助教。</p>
          <p>&#169 2026 智问学伴在线教育系统 · 保留所有权利</p>
        </div>
        <div class="logo">
          <div class="footer-logo">
            <div class="footer-logo-icon">智</div>
            <div class="footer-logo-text">智问学伴</div>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
// 站内-友情链接
const Links = [
  { title: '关于我们', link: '/' },
  { title: '管理团队', link: '/' },
  { title: '讲师入驻', link: '/' },
  { title: '客户服务', link: '/' },
  { title: '帮助文档', link: '/' },
  { title: '选课指引', link: '/' },
  { title: '企业培训', link: '/' },
  { title: '合作机构', link: '/' }
]

const goDetails = (link) => {
  return false;
}
</script>

<style lang="scss" scoped>
footer {
  width: 100%;
  background-color: #FFFFFF;
  border-top: 1px solid #E2E8F0;
  text-align: left;
  padding: 40px 0;
  font-size: 13px;

  .links {
    margin-bottom: 24px;
    display: flex;
    flex-wrap: wrap;
    gap: 24px;

    span {
      color: #64748B;
      cursor: pointer;
      transition: color 0.2s ease;
      &:hover {
        color: #2563EB;
      }
    }
  }

  .about {
    line-height: 1.8;
    color: #64748B;
    align-items: center;

    .info p {
      margin: 2px 0;
    }
  }

  .footer-logo {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .footer-logo-icon {
    width: 36px;
    height: 36px;
    background: #2563EB;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #FFFFFF;
    font-size: 18px;
    font-weight: 700;
  }

  .footer-logo-text {
    font-size: 17px;
    font-weight: 700;
    color: #0F172A;
  }
}

@media (max-width: 768px) {
  footer {
    padding: 24px 0;
    .about {
      flex-direction: column-reverse;
      align-items: flex-start;
      gap: 16px;
    }
  }
}
</style>
