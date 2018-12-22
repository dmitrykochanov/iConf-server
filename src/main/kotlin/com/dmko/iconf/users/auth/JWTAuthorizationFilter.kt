package com.dmko.iconf.users.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.util.stream.Collectors
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(authManager: AuthenticationManager) : BasicAuthenticationFilter(authManager) {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header: String? = request.getHeader(AuthConstants.HEADER_STRING)
        if (header == null || !header.startsWith(AuthConstants.TOKEN_PREFIX)) {
            chain.doFilter(request, response)
            return
        }

        val auth = getAuth(request)
        SecurityContextHolder.getContext().authentication = auth
        chain.doFilter(request, response)
    }

    private fun getAuth(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val token: String? = request.getHeader(AuthConstants.HEADER_STRING)
        if (token != null) {


            val parsedToken = JWT.require(Algorithm.HMAC512(AuthConstants.SECRET.toByteArray()))
                    .build()
                    .verify(token.replace(AuthConstants.TOKEN_PREFIX, ""))


            if (parsedToken.subject != null) {

                val roles = parsedToken.getClaim(AuthConstants.AUTHORITIES_KEY)
                        .asString()
                        .split(",")
                        .stream()
                        .map { SimpleGrantedAuthority(it) }
                        .collect(Collectors.toList())
                return UsernamePasswordAuthenticationToken(parsedToken.subject, null, roles)
            }
        }
        return null
    }
}