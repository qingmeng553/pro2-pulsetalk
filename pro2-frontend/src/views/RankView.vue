<template>
  <div class="page-container rank-page">
    <div class="section-title">
      帖子热度排行榜
      <el-button size="small" circle :icon="Refresh" title="刷新" @click="load" />
    </div>

    <div class="chart-card">
      <div class="chart-tip">热度 = 点赞×3 + 收藏×5 + 浏览×0.1　·　点击条形可直接进入对应帖子</div>
      <div ref="chartRef" class="chart"></div>
      <div v-if="!rows.length" class="empty-tip">暂无热度数据</div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { apiHotRank } from '../api/post'

const rows = ref([])
const chartRef = ref()
const router = useRouter()

let chart = null

// 拉取榜单并渲染
async function load() {
  try {
    rows.value = await apiHotRank(20)
    renderChart()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

function renderChart() {
  const el = chartRef.value
  if (!el) return
  if (!chart) chart = echarts.init(el)

  // 横向条形图自下而上展示，取正序
  const asc = [...rows.value].sort((a, b) => (a.score || 0) - (b.score || 0))
  const labels = asc.map((p, i) => `${i + 1}. ${p.title.length > 16 ? p.title.slice(0, 16) + '…' : p.title}`)

  chart.setOption({
    grid: { left: 12, right: 70, top: 18, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = asc[params[0].dataIndex]
        if (!p) return ''
        return `${p.title}<br/>热度 <b>${Number(p.score).toFixed(1)}</b><br/>点赞 ${p.likeCount} · 收藏 ${p.collectCount} · 浏览 ${p.viewCount}<br/><span style="color:#8b95b2">点击查看帖子</span>`
      }
    },
    xAxis: {
      type: 'value',
      name: '热度',
      nameTextStyle: { color: '#9aa3b8' },
      axisLabel: { color: '#9aa3b8' },
      splitLine: { lineStyle: { color: '#eef1f8' } }
    },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: '#4a5268', fontSize: 12 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: asc.map((p) => Number((p.score || 0).toFixed(1))),
      barWidth: 16,
      cursor: 'pointer',
      itemStyle: {
        borderRadius: [0, 8, 8, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#8b5cf6' },
          { offset: 1, color: '#5b7cfa' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#a78bfa' },
            { offset: 1, color: '#7c9bff' }
          ])
        }
      },
      label: { show: true, position: 'right', color: '#5b7cfa', fontWeight: 600 },
      animationDuration: 800,
      animationEasing: 'cubicOut'
    }]
  }, true)

  // 点击柱子跳转帖子
  chart.off('click')
  chart.on('click', (params) => {
    const item = asc[params.dataIndex]
    if (item) router.push(`/post/${item.postId}`)
  })
}

function resizeChart() {
  chart && chart.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chart) chart.dispose()
  chart = null
})
</script>

<style scoped>
.rank-page {
  max-width: 1080px;
}
.chart-card {
  position: relative;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px 18px 10px;
}
.chart-tip {
  font-size: 12.5px;
  color: var(--text-sub);
  margin-bottom: 6px;
}
.chart {
  width: 100%;
  height: 620px;
}

@media (max-width: 768px) {
  .chart {
    height: 480px;
  }
  .chart-card {
    padding: 12px 10px 6px;
    border-radius: 14px;
  }
}
@media (max-width: 420px) {
  .chart {
    height: 420px;
  }
}
</style>
