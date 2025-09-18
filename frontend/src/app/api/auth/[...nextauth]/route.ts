import NextAuth from "next-auth";
import Google from "next-auth/providers/google";

const handler = NextAuth({
  providers: [
    Google({
      clientId: process.env.GOOGLE_CLIENT_ID || "",
      clientSecret: process.env.GOOGLE_CLIENT_SECRET || "",
    }),
  ],
  session: { strategy: "jwt" },
  callbacks: {
    async jwt({ token, account }) {
      if (account && account.id_token) {
        (token as { idToken?: string }).idToken = account.id_token;
      }
      return token;
    },
    async session({ session, token }) {
      (session as { idToken?: string }).idToken = (token as { idToken?: string }).idToken;
      return session;
    },
  },
});

export { handler as GET, handler as POST };
