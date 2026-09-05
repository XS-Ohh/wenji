<script setup lang="ts">
import { Location, MapLocation, Refresh, Search, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import L, { type Map as LeafletMap, type Marker } from 'leaflet'
import 'leaflet.markercluster'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { resourceApi } from '../api/resources'
import type { Category, CultureMapResource } from '../types/api'

const SHANGHAI_CENTER: L.LatLngExpression = [31.2304, 121.4737]
const tileUrl = import.meta.env.VITE_MAP_TILE_URL || 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const attribution = import.meta.env.VITE_MAP_ATTRIBUTION
  || '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
const palette = ['#416c85', '#8a594f', '#5b7653', '#8b6f42', '#665b82', '#a65347']

const router = useRouter()
const mapElement = ref<HTMLElement>()
const loading = ref(true)
const failed = ref(false)
const tileFailed = ref(false)
const resources = ref<CultureMapResource[]>([])
const categories = ref<Category[]>([])
const activeId = ref<number>()
const visibleLocatedCount = ref(0)
const resourcePanel = ref<HTMLElement>()
const filters = reactive({ keyword: '', categoryId: undefined as number | undefined, city: '上海' })

let map: LeafletMap | undefined
let clusters: L.MarkerClusterGroup | undefined
const markerById = new Map<number, Marker>()

const locatedResources = computed(() => resources.value.filter(hasCoordinates))
const missingCoordinateCount = computed(() => resources.value.length - locatedResources.value.length)

function hasCoordinates(resource: CultureMapResource) {
  return Number.isFinite(resource.latitude) && Number.isFinite(resource.longitude)
    && Math.abs(resource.latitude || 0) <= 90 && Math.abs(resource.longitude || 0) <= 180
}

function markerColor(categoryId: number) {
  return palette[(categoryId - 1 + palette.length) % palette.length]
}

function createMarkerIcon(resource: CultureMapResource) {
  const color = markerColor(resource.categoryId)
  return L.divIcon({
    className: 'culture-marker-host',
    html: `<span class="culture-marker-pin" style="--marker-color:${color}"><i></i></span>`,
    iconSize: [34, 42],
    iconAnchor: [17, 40],
    popupAnchor: [0, -36],
  })
}

function appendText(parent: HTMLElement, tag: keyof HTMLElementTagNameMap, className: string, value?: string) {
  if (!value) return
  const element = document.createElement(tag)
  element.className = className
  element.textContent = value
  parent.appendChild(element)
}

function popupContent(resource: CultureMapResource) {
  const root = document.createElement('article')
  root.className = 'culture-map-popup'
  appendText(root, 'span', 'culture-map-popup-category', resource.categoryName)
  appendText(root, 'h3', '', resource.name)
  appendText(root, 'p', 'culture-map-popup-address', resource.address)
  appendText(root, 'p', 'culture-map-popup-summary', resource.summary)
  const meta = [
    resource.averageRating ? `评分 ${resource.averageRating.toFixed(1)}` : '',
    `收藏 ${resource.favoriteCount}`,
  ].filter(Boolean).join(' · ')
  appendText(root, 'p', 'culture-map-popup-meta', meta)
  appendText(root, 'p', 'culture-map-popup-hours', resource.openingHours ? `开放时间：${resource.openingHours}` : '')
  const link = document.createElement('a')
  link.href = `/resources/${resource.id}`
  link.textContent = '查看资源详情 →'
  link.addEventListener('click', (event) => {
    event.preventDefault()
    router.push(`/resources/${resource.id}`)
  })
  root.appendChild(link)
  return root
}

function initializeMap() {
  if (!mapElement.value || map) return
  map = L.map(mapElement.value, { zoomControl: false, minZoom: 3, maxZoom: 19 }).setView(SHANGHAI_CENTER, 11)
  L.control.zoom({ position: 'bottomright' }).addTo(map)
  L.tileLayer(tileUrl, { attribution, maxZoom: 19 }).on('tileerror', () => {
    tileFailed.value = true
  }).addTo(map)
  clusters = L.markerClusterGroup({
    showCoverageOnHover: false,
    maxClusterRadius: 48,
    spiderfyOnMaxZoom: true,
    iconCreateFunction(cluster) {
      return L.divIcon({
        className: 'culture-cluster-host',
        html: `<span>${cluster.getChildCount()}</span>`,
        iconSize: [42, 42],
      })
    },
  })
  map.addLayer(clusters)
  map.on('moveend', updateVisibleCount)
}

function updateVisibleCount() {
  if (!map) {
    visibleLocatedCount.value = 0
    return
  }
  const bounds = map.getBounds()
  visibleLocatedCount.value = locatedResources.value.filter((resource) => (
    bounds.contains([resource.latitude!, resource.longitude!])
  )).length
}

function activateResource(resourceId: number) {
  activeId.value = resourceId
  markerById.forEach((marker, id) => {
    marker.getElement()?.classList.toggle('is-active', id === resourceId)
  })
  nextTick(() => {
    resourcePanel.value
      ?.querySelector<HTMLElement>(`[data-resource-id="${resourceId}"]`)
      ?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function renderMarkers(fit = true) {
  if (!map || !clusters) return
  clusters.clearLayers()
  markerById.clear()
  const bounds: L.LatLngExpression[] = []
  locatedResources.value.forEach((resource) => {
    const position: L.LatLngExpression = [resource.latitude!, resource.longitude!]
    const marker = L.marker(position, {
      icon: createMarkerIcon(resource),
      title: resource.name,
      alt: resource.name,
      keyboard: true,
    }).bindPopup(popupContent(resource), { maxWidth: 300 })
    marker.on('click', () => activateResource(resource.id))
    markerById.set(resource.id, marker)
    clusters!.addLayer(marker)
    bounds.push(position)
  })
  if (!fit) return
  if (bounds.length === 1) map.setView(bounds[0], 15)
  else if (bounds.length > 1) map.fitBounds(L.latLngBounds(bounds), { padding: [42, 42], maxZoom: 15 })
  else map.setView(SHANGHAI_CENTER, 11)
  updateVisibleCount()
}

function showAllResources() {
  const positions = locatedResources.value.map((resource) => (
    [resource.latitude!, resource.longitude!] as L.LatLngExpression
  ))
  if (!map || positions.length === 0) return
  if (positions.length === 1) map.setView(positions[0], 15)
  else map.fitBounds(L.latLngBounds(positions), { padding: [42, 42], maxZoom: 15 })
}

async function load() {
  loading.value = true
  failed.value = false
  try {
    activeId.value = undefined
    resources.value = await resourceApi.map({
      keyword: filters.keyword.trim() || undefined,
      categoryId: filters.categoryId,
      city: filters.city.trim() || undefined,
    })
    await nextTick()
    renderMarkers()
  } catch {
    failed.value = true
    ElMessage.error('文化地图加载失败')
  } finally {
    loading.value = false
  }
}

function focusResource(resource: CultureMapResource) {
  if (!map || !clusters) return
  const marker = markerById.get(resource.id)
  if (!marker) return
  activateResource(resource.id)
  clusters.zoomToShowLayer(marker, () => {
    map!.setView(marker.getLatLng(), Math.max(map!.getZoom(), 15), { animate: true })
    activateResource(resource.id)
    marker.openPopup()
  })
}

function resetFilters() {
  Object.assign(filters, { keyword: '', categoryId: undefined, city: '上海' })
  load()
}

onMounted(async () => {
  initializeMap()
  const categoryResult = await Promise.allSettled([resourceApi.categories(), load()])
  if (categoryResult[0].status === 'fulfilled') categories.value = categoryResult[0].value
  window.setTimeout(() => map?.invalidateSize(), 0)
})

onBeforeUnmount(() => {
  map?.remove()
  map = undefined
  clusters = undefined
  markerById.clear()
})
</script>

<template>
  <section class="culture-map-page">
    <header class="section-heading map-page-heading">
      <div><p class="eyebrow">CULTURAL ATLAS</p><h1>文化地图</h1><p>在城市地图中发现已记录的文化资源</p></div>
      <div class="map-result-summary">
        <strong>{{ locatedResources.length }}</strong><span>处已定位</span>
        <small v-if="missingCoordinateCount">{{ missingCoordinateCount }} 处缺少坐标</small>
      </div>
    </header>

    <form class="map-filter-bar" @submit.prevent="load">
      <el-input v-model="filters.keyword" clearable placeholder="搜索名称、简介或地址" :prefix-icon="Search" />
      <el-select v-model="filters.categoryId" clearable placeholder="全部分类" @change="load">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <el-input v-model="filters.city" clearable placeholder="城市" />
      <el-button type="primary" native-type="submit" :icon="Search">定位资源</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </form>

    <el-alert v-if="tileFailed" class="map-tile-warning" type="warning" :closable="false" show-icon title="底图服务暂时不可用，资源标记仍可通过左侧列表查看；可配置其他瓦片服务。" />

    <div v-loading="loading" class="culture-map-stage">
      <aside ref="resourcePanel" class="map-resource-panel" aria-label="地图资源列表">
        <div class="map-panel-heading"><MapLocation /><span>当前结果</span><em>{{ resources.length }}</em></div>
        <el-result v-if="failed" icon="error" title="加载失败"><template #extra><el-button @click="load">重试</el-button></template></el-result>
        <el-empty v-else-if="!loading && resources.length === 0" description="暂无符合条件的文化资源" />
        <div v-else class="map-resource-list">
          <button
            v-for="resource in resources"
            :key="resource.id"
            :data-resource-id="resource.id"
            type="button"
            class="map-resource-item"
            :class="{ active: activeId === resource.id, disabled: !hasCoordinates(resource) }"
            :disabled="!hasCoordinates(resource)"
            @click="focusResource(resource)"
          >
            <span class="map-resource-dot" :style="{ background: markerColor(resource.categoryId) }"></span>
            <span class="map-resource-copy">
              <strong>{{ resource.name }}</strong>
              <small>{{ resource.categoryName }} · {{ resource.district || resource.city }}</small>
              <span><Location />{{ resource.address }}</span>
              <em v-if="!hasCoordinates(resource)">暂缺经纬度</em>
            </span>
            <span v-if="resource.averageRating" class="map-resource-rating"><Star />{{ resource.averageRating.toFixed(1) }}</span>
          </button>
        </div>
      </aside>
      <div ref="mapElement" class="culture-map-canvas" aria-label="文化资源交互地图"></div>
      <div v-if="locatedResources.length" class="map-viewport-status">
        <span>当前视野 <strong>{{ visibleLocatedCount }}</strong> / {{ locatedResources.length }}</span>
        <button type="button" @click="showAllResources">显示全部</button>
      </div>
    </div>
  </section>
</template>
