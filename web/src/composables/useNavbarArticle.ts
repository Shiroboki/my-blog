import { ref } from "vue";

export interface NavbarArticleState {
  title: string;
  category?: string;
  absorbed: boolean;
}

const navbarArticle = ref<NavbarArticleState | null>(null);

export function useNavbarArticle() {
  function setNavbarArticle(state: NavbarArticleState | null) {
    navbarArticle.value = state;
  }

  return {
    navbarArticle,
    setNavbarArticle,
  };
}
