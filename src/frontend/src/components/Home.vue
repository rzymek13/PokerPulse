<template>
  <div class="container">
    <div class="panel" style="max-width:760px; margin:40px auto; text-align:center;">
      <h1>Witaj w PokerPulse</h1>
      <p class="subtitle">Wybierz jedną z opcji, aby przejść dalej</p>

      <div class="grid grid-3 mt-24">
        <a class="panel" href="/sivi.html" target="_blank" style="text-decoration:none">
          <h2>O mnie</h2>
          <p class="label">CV — umiejętności i narzędzia</p>
        </a>

        <a class="panel" href="https://github.com/rzymek13" target="_blank" rel="noopener">
          <h2>GitHub</h2>
          <p class="label">github.com/rzymek13</p>
        </a>

        <router-link class="panel" to="/roomList" style="text-decoration:none">
          <h2>PokerPulse</h2>
          <p class="label">Przejdź do aplikacji</p>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default { name: 'Home' };
</script>
