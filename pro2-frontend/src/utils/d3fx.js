import * as d3 from 'd3'

/**
 * D3.js 动效工具集
 *
 * 原则：适度娱乐化 —— 只做“入场、hover、榜单换位”三类轻量动效，
 * 拒绝无意义的持续花哨特效。
 */

/**
 * 列表卡片入场动画：自上而下错落淡入 + 轻微上浮
 * @param rootSelector 容器选择器(容器内固定包含 .anim-item 子元素)
 * @param delayMs 相邻卡片起始延迟
 */
export function runCardEntrance(rootSelector, delayMs = 60) {
  d3.select(rootSelector)
    .selectAll('.anim-item')
    .style('opacity', 0)
    .attr('data-ready', '0')
    .transition()
    .delay((d, i) => i * delayMs)
    .duration(380)
    .ease(d3.easeCubicOut)
    .style('opacity', 1)
    .attr('data-ready', '1')
}

/**
 * 通用 hover 上浮效果：为指定选择器内的卡片绑定 mouseenter/mouseleave 过渡
 * @param selector 卡片选择器(如 .post-card)
 */
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

/**
 * 榜单条目数据联动渲染(enter 插入 / update 换位 / exit 移除均带过渡)
 * @param listSelector 条目容器选择器
 * @param data 数据数组
 * @param keyFn 唯一键函数
 * @param render 渲染函数: (enterSel) => 对新进入元素做内容写入并返回它
 */
export function renderRankList(listSelector, data, keyFn, render) {
  const rows = d3.select(listSelector)
    .selectAll('.rank-row')
    .data(data, keyFn)

  // exit：旧条目淡出移除
  rows.exit()
    .transition()
    .duration(260)
    .style('opacity', 0)
    .style('transform', 'translateX(-18px)')
    .remove()

  // enter：先置入，再做入场过渡
  const enter = rows.enter()
    .append('div')
    .attr('class', 'rank-row anim-item')
    .style('opacity', 0)
    .style('transform', 'translateY(14px)')

  render(enter)

  // enter + update 统一过渡到就位态
  enter.merge(rows)
    .transition()
    .duration(420)
    .ease(d3.easeCubicOut)
    .style('opacity', 1)
    .style('transform', 'translateX(0) translateY(0)')
}

/** 千分位数字格式 */
export function numberFormat(n) {
  return d3.format(',')(n || 0)
}
