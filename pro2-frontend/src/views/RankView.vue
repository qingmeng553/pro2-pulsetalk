<template>
  <div class="page-container rank-page">
    <div class="section-title">
      帖子热度排行榜
      <el-button size="small" circle :icon="Refresh" title="刷新" @click="load" />
    </div>

    <div class="rank-grid">
      <!-- 左：ECharts 热度统计图表 -->
      <div class="chart-card">
        <div ref="chartRef" class="chart"></div>
        <div v-if="!rows.length" class="empty-tip">暂无热度数据，去发帖互动吧 🚀</div>
      </div>

      <!-- 右：D3 动态渲染榜单 -->
      <div class="list-card">
        <div ref="listRef" class="rank-list"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import * as d3 from 'd3'
import { ElMessage } from 'element-plus'
import { apiHotRank } from '../api/post'
import { fmtCount } from '../utils/format'

const rows = ref([])
const chartRef = ref()
const listRef = ref()
const router = useRouter()

let chart = null

/** 抓取榜单 */
async function load() {
  try {
    rows.value = await apiHotRank(10)
    await renderChart()
    renderRows()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

/** ECharts：Top10 热度横向条形图 */
async function renderChart() {
  const el = chartRef.value
  if (!el) return
  if (!chart) {
    chart = echarts.init(el)
  }
  // 热度降序 → 图表自下而上展示，正序数据即可
  const asc = [...rows.value].sort((a, b) => (a.score || 0) - (b.score || 0))
  const option = {
    grid: { left: 10, right: 40, top: 20, bottom: 10, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = asc[params[0].dataIndex]
        if (!p) return ''
        return `${p.title}<br/>热度分 <b>${Number(p.score).toFixed(1)}</b><br/>点赞 ${fmtCount(p.likeCount)} · 收藏 ${fmtCount(p.collectCount)} · 浏览 ${fmtCount(p.viewCount)}`
      }
    },
    xAxis: { type: 'value', name: '热度分', axisLabel: { color: '#9aa3b8' }, splitLine: { lineStyle: { color: '#eef1f8' } } },
    yAxis: {
      type: 'category',
      data: asc.map((p) => (p.title.length > 12 ? p.title.slice(0, 12) + '…' : p.title)),
      axisLabel: { color: '#4a5268', fontSize: 12 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        type: 'bar',
        data: asc.map((p) => Number((p.score || 0).toFixed(1))),
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#8b5cf6' },
            { offset: 1, color: '#5b7cfa' }
          ])
        },
        label: { show: true, position: 'right', color: '#5b7cfa', fontWeight: 600 },
        animationDuration: 900,
        animationEasing: 'cubicOut'
      }
    ]
  }
  chart.setOption(option)
  // 点击柱子跳详情
  chart.off('click')
  chart.on('click', (params) => {
    const item = asc[params.dataIndex]
    if (item) router.push(`/post/${item.postId}`)
  })
}

/** D3 榜单条目动态渲染：带名次徽章、错落入场、hover 上浮 */
function renderRows() {
  const el = listRef.value
  if (!el) return
  d3.select(el).selectAll('*').remove()

  const medal = (i) => (i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : String(i + 1))

  const rowsSel = d3.select(el)
    .selectAll('div')
    .data(rows.value, (d) => d.postId)
    .enter()
    .append('div')
    .attr('class', 'rank-row anim-item')
    .style('opacity', 0)
    .style('transform', 'translateY(16px)')
    .style('cursor', 'pointer')
    .on('click', (e, d) => router.push(`/post/${d.postId}`))
    .on('mouseenter', function () {
      d3.select(this).transition().duration(160).style('transform', 'translateX(6px)')
    })
    .on('mouseleave', function () {
      d3.select(this).transition().duration(160).style('transform', 'translateX(0)')
    })

  // 内容布局
  rowsSel.each(function (d, i) {
    const row = d3.select(this)
    row.append('div').attr('class', 'rank-no').text(medal(i))
    const main = row.append('div').attr('class', 'rank-main')
    main.append('div').attr('class', 'rank-title').text(d.title)
    const meta = main.append('div').attr('class', 'rank-meta')
    meta.append('span').text(`${d.categoryName || '综合'}`)
    meta.append('span').text(`👍 ${fmtCount(d.likeCount)}`)
    meta.append('span').text(`⭐ ${fmtCount(d.collectCount)}`)
    meta.append('span').text(`👀 ${fmtCount(d.viewCount)}`)
    row.append('div').attr('class', 'rank-score').text(Number(d.score).toFixed(1))
  })

  // 错落入场
  rowsSel
    .transition()
    .delay((d, i) => i * 60)
    .duration(420)
    .ease(d3.easeCubicOut)
    .style('opacity', 1)
    .style('transform', 'translateY(0)')
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
  chart && chart.dispose()
})
</script>

<style scoped>
.rank-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 16px;
}
.chart-card,
.list-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
}
.chart {
  width: 100%;
  height: 440px;
}
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 440px;
  overflow-y: auto;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f7f9ff;
  border: 1px solid transparent;
  border-radius: 12px;
  padding: 10px 12px;
  transition: border-color 0.2s, background 0.2s;
}
.rank-row:hover {
  border-color: var(--brand);
  background: #f0f3ff;
}
.rank-no {
  width: 34px;
  text-align: center;
  font-size: 17px;
  font-weight: 800;
  color: var(--brand);
}
.rank-main {
  flex: 1;
  min-width: 0;
}
.rank-title {
  font-weight: 600;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rank-meta {
  margin-top: 3px;
  display: flex;
  gap: 10px;
  font-size: 11.5px;
  color: var(--text-sub);
}
.rank-score {
  font-size: 16px;
  font-weight: 800;
  color: var(--brand);
}
@media (max-width: 860px) {
  .rank-grid {
    grid-template-columns: 1fr;
  }
}
</style>
