package com.apsmkimo.go2radiolive.data.remote

import java.util.Locale

/**
 * Apple podcast chart storefronts (iTunes country codes). `gb` is the United Kingdom.
 * Device regions outside this set have no Apple chart.
 */
internal object AppleStorefronts {
    val codes: List<String> = CODES.split(',')

    private val lookup: Set<String> = codes.toHashSet()

    fun supports(code: String?): Boolean = code?.trim()?.lowercase() in lookup
}

internal fun storefrontDisplayName(code: String, locale: Locale): String {
    val region = code.trim().uppercase(Locale.US)
    if (region.length != 2) return region
    val name = Locale("", region).getDisplayCountry(locale)
    if (name.isBlank() || name.equals(region, ignoreCase = true)) return region
    return name
}

private const val CODES =
    "ae,ag,ai,al,am,ao,ar,at,au,az," +
        "bb,be,bf,bg,bh,bj,bm,bn,bo,br,bs,bt,bw,by,bz," +
        "ca,cg,ch,cl,cn,co,cr,cv,cy,cz," +
        "de,dk,dm,do,dz," +
        "ec,ee,eg,es," +
        "fi,fj,fm,fr," +
        "gb,gd,gh,gm,gr,gt,gw,gy," +
        "hk,hn,hr,hu," +
        "id,ie,il,in,is,it," +
        "jm,jo,jp," +
        "ke,kg,kh,kn,kr,kw,ky,kz," +
        "la,lb,lc,lk,lr,lt,lu,lv," +
        "md,mg,mk,ml,mn,mo,mr,ms,mt,mu,mw,mx,my,mz," +
        "na,ne,ng,ni,nl,no,np,nz," +
        "om," +
        "pa,pe,pg,ph,pk,pl,pt,pw,py," +
        "qa," +
        "ro,ru," +
        "sa,sb,sc,se,sg,si,sk,sl,sn,sr,st,sv,sz," +
        "tc,td,th,tj,tm,tn,tr,tt,tw,tz," +
        "ua,ug,us,uy,uz," +
        "vc,ve,vg,vn," +
        "ye," +
        "za,zw"
