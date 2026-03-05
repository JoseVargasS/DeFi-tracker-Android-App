package com.defitracker.app.core

object Constants {
    const val BINANCE_BASE_URL = "https://api.binance.com/api/v3/"
    const val COINSTATS_BASE_URL = "https://openapiv1.coinstats.app/"
    const val COINSTATS_API_KEY = "zaahGoIVFiB69a4tlS5jIchyt+YYNNmMW4cdw0Lcto0="
    
    // Etherscan API V2 (solo Ethereum en plan gratuito)
    const val ETHERSCAN_V2_BASE_URL = "https://api.etherscan.io/"

    // APIs nativas de cada explorador (V1) para BSC, Polygon, Base, etc.
    const val BSC_SCAN_BASE_URL = "https://api.bscscan.com/"
    const val POLYGON_SCAN_BASE_URL = "https://api.polygonscan.com/"
    const val BASE_SCAN_BASE_URL = "https://api.basescan.org/"
    const val OPTIMISM_SCAN_BASE_URL = "https://api-optimistic.etherscan.io/"
    const val ARBISCAN_BASE_URL = "https://api.arbiscan.io/"

    const val ETHERSCAN_API_KEY = "3UZBJN7R92B2QM1Q7PCGRY8JNJ4A55RDJT"

    // Moralis: tier gratuito con BSC, Polygon, Base, etc. Obtener key en https://admin.moralis.io/register
    const val MORALIS_BASE_URL = "https://deep-index.moralis.io/"
    const val MORALIS_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJub25jZSI6IjZhMWQ5OTYzLWIwYjUtNDk1Zi04NmNkLWVlN2QwNWQwZjc2OSIsIm9yZ0lkIjoiNTAxMTM0IiwidXNlcklkIjoiNTE1NjM4IiwidHlwZUlkIjoiNWNkNDYwYmUtNmMwYS00YWViLTk3OGYtODU3YTNlODgzMTA0IiwidHlwZSI6IlBST0pFQ1QiLCJpYXQiOjE3NzEzODM4MzYsImV4cCI6NDkyNzE0MzgzNn0.ot2Lssj0VHnEOSB9D-lC56PU3mtsOYEKq4RLELVN8K4" // Poner tu API key de Moralis para ver transacciones en BSC, Polygon, Base, Optimism, Arbitrum
    const val HTX_BASE_URL = "https://api.huobi.pro/"
    
    const val DATABASE_NAME = "defi_tracker_db"
}
