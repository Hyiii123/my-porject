<!-- 课程展示卡片 - 首页、搜索页 -->
<template>
  <div class="classCards" @click="goDetails(data.id)">
    <div class="corner c1"></div>
    <div class="corner c2"></div>
    <div class="image-wrapper">
      <img :src="data.coverUrl || data.courseCoverUrl || data.cover || defaultCover" alt="课程封面" class="cover-img" loading="lazy" @error="handleImgError" />
      <span v-if="data.categoryName" class="category-badge">{{ data.categoryName }}</span>
    </div>
    <div class="card-content">
      <div class="title" :title="data.title || data.courseName || data.name" v-html="data.title || data.courseName || data.name"></div>

      <div class="meta-info">
        <span class="teacher" v-if="data.teacherName || data.teacher">讲师：{{ data.teacherName || data.teacher }}</span>
        <span class="sections" v-if="data.lessonCount || data.lessons || data.sections">共 {{ data.lessonCount || data.lessons || data.sections }} 节</span>
      </div>

      <div class="card-footer">
        <span class="learners">{{ data.learnerCount || data.learners || data.sold || 0 }} 人在学</span>
        <div class="price-wrapper">
          <span v-if="Number(data.price) > 0" class="price">
            <small>¥</small>{{ (Number(data.price) / 100).toFixed(2) }}
          </span>
          <span v-else class="price-free">免费学习</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import defaultCover from '@/assets/images/courses/default-cover.svg?url';

const router = useRouter();
const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  type: {
    type: String,
    default: 'default'
  }
});

const handleImgError = (e) => {
  if (props?.data) {
    props.data.coverUrl = defaultCover;
    props.data.courseCoverUrl = defaultCover;
    props.data.cover = defaultCover;
  }
  if (e?.target && e.target.src !== defaultCover) {
    e.target.src = defaultCover;
  }
};

const goDetails = id => {
  if (id) {
    router.push({ path: '/details', query: { id } });
  }
};
</script>

<style lang="scss" scoped>
.classCards {
  position: relative;
  background: rgba(16, 23, 38, 0.72);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 12px 32px -8px rgba(0, 0, 0, 0.6);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  .corner {
    position: absolute;
    width: 12px;
    height: 12px;
    border: 2px solid #38BDF8;
    opacity: 0;
    transition: opacity 0.25s ease;
    pointer-events: none;
    z-index: 5;

    &.c1 { top: 8px; left: 8px; border-right: none; border-bottom: none; }
    &.c2 { bottom: 8px; right: 8px; border-left: none; border-top: none; }
  }

  &:hover {
    border-color: rgba(56, 189, 248, 0.45);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2), 0 20px 48px -10px rgba(14, 165, 233, 0.3);
    transform: translateY(-4px);

    .corner {
      opacity: 0.85;
    }
    .image-wrapper .cover-img {
      transform: scale(1.05);
    }
    .title {
      color: #38BDF8;
    }
  }

  .image-wrapper {
    width: 100%;
    height: 160px;
    position: relative;
    overflow: hidden;
    background: #0B0F19;

    .cover-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    }

    .category-badge {
      position: absolute;
      top: 10px;
      left: 10px;
      background: rgba(7, 9, 14, 0.75);
      border: 1px solid rgba(255, 255, 255, 0.12);
      backdrop-filter: blur(8px);
      color: #38BDF8;
      font-size: 11px;
      font-family: var(--mono);
      font-weight: 600;
      padding: 3px 9px;
      border-radius: 6px;
      letter-spacing: 0.04em;
    }
  }

  .card-content {
    padding: 16px 18px;
    display: flex;
    flex-direction: column;
    flex: 1;

    .title {
      font-size: 14.5px;
      font-weight: 600;
      line-height: 1.45;
      color: #F8FAFC;
      margin-bottom: 8px;
      height: 42px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-overflow: ellipsis;
      transition: color 0.2s ease;
      letter-spacing: -0.01em;

      :deep(em) {
        font-style: normal;
        color: #38BDF8;
        font-weight: 700;
      }
    }

    .meta-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      color: #94A3B8;
      margin-bottom: 14px;

      .teacher {
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .sections {
        position: relative;
        padding-left: 8px;
        font-family: var(--mono);
        color: #64748B;
        &::before {
          content: '•';
          position: absolute;
          left: 0;
          color: rgba(255, 255, 255, 0.2);
        }
      }
    }

    .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: auto;
      padding-top: 12px;
      border-top: 1px solid rgba(255, 255, 255, 0.08);

      .learners {
        font-size: 11.5px;
        color: #64748B;
        font-family: var(--mono);
      }

      .price-wrapper {
        .price {
          font-size: 18px;
          font-weight: 700;
          font-family: var(--mono);
          color: #34D399;
          small {
            font-size: 12px;
            font-weight: 600;
            margin-right: 2px;
          }
        }
        .price-free {
          font-size: 12.5px;
          font-weight: 600;
          color: #38BDF8;
          background: rgba(56, 189, 248, 0.12);
          border: 1px solid rgba(56, 189, 248, 0.25);
          padding: 3px 9px;
          border-radius: 6px;
        }
      }
    }
  }
}
</style>