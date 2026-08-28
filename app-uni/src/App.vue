<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";
import { useLocationStore } from "@/store/location";
import { setupInterceptors } from "@/utils/interceptor";

onLaunch(() => {
  useUserStore().restore();
  useLocationStore().restore();
  setupInterceptors();
});
</script>

<style lang="scss">
page {
  /* wot-design-uni 主题色 */
  --wot-color-theme: #07c160;
  --wot-color-success: #07c160;
  --wot-button-primary-bg-color: #07c160;
  background-color: #f5f6f8;
  font-size: 28rpx;
  color: #1f2329;
}
</style>
