import * as d3 from 'd3'

// 轻量动效：入场、hover、榜单渲染

// 列表卡片错落入场
export function runCardEntrance(rootSelector, delayMs = 60) {
  d3.select(rootSelector)
    .selectAll('.anim-item')
    .style('opacity', 0)
    .transition()
    .delay((d, i) => i * delayMs)
    .duration(380)
    .ease(d3.easeCubicOut)
    .style('opacity', 1)
}

// 卡片 hover 上浮
export function bindHoverLift(selector) {
  const lift = (node, dy) => {
    d3.select(node)
      .transition()
      .duration(180)
      .ease(d3.easeQuadOut)
      .style('transform', `translateY(${dy}px)`)
      .style('box-shadow', dy === 0
        ? '0 2px 8px rgba(0,0,0,.06)'
        : '0 10px 24px rgba(80, 110, 255, .16)')
  }
  d3.selectAll(selector)
    .on('mouseenter', function () { lift(this, -4) })
    .on('mouseleave', function () { lift(this, 0) })
}

// 榜单条目数据联动（enter / update / exit 均带过渡）
export function renderRankList(listSelector, data, keyFn, render) {
  const rows = d3.select(listSelector)
    .selectAll('.rank-row')
    .data(data, keyFn)

  rows.exit()
    .transition()
    .duration(260)
    .style('opacity', 0)
    .remove()

  const enter = rows.enter()
    .append('div')
    .attr('class', 'rank-row anim-item')
    .style('opacity', 0)
    .style('transform', 'translateY(14px)')

  render(enter)

  enter.merge(rows)
    .transition()
    .duration(420)
    .ease(d3.easeCubicOut)
    .style('opacity', 1)
    .style('transform', 'translateX(0) translateY(0)')
}

// 千分位
export function numberFormat(n) {
  return d3.format(',')(n || 0)
}
