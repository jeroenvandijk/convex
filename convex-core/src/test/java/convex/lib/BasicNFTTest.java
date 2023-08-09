package convex.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import convex.core.crypto.AKeyPair;
import convex.core.data.AVector;
import convex.core.data.Address;
import convex.core.data.Sets;
import convex.core.data.Vectors;
import convex.core.data.prim.CVMLong;
import convex.core.lang.ACVMTest;
import convex.core.lang.Context;
import convex.core.lang.TestState;
import static convex.test.Assertions.*;

public class BasicNFTTest extends ACVMTest {
	
	static final AKeyPair KP1=AKeyPair.generate();
	static final AKeyPair KP2=AKeyPair.generate();
	
	Address NFT;
	
	@Override protected Context buildContext(Context ctx) {
		ctx=TestState.CONTEXT.fork();
		String importS = "(import asset.nft.basic :as nft)";
		ctx=step(ctx,importS);
		NFT=ctx.getResult();
		
		ctx=step(ctx,"(import convex.asset :as asset)");
		return ctx;
	}
	
	@Test public void testMetadata() {
		Context ctx=context();
		ctx=step(ctx,"(def t1 (call nft (create [1 2])))");
		
		assertEquals(Vectors.of(1,2),eval(ctx,"(call [nft t1] (get-metadata))"));
		assertEquals(Vectors.of(1,2),eval(ctx,"(call nft (get-metadata t1))"));
		
		ctx=step(ctx,"(def t2 (call nft (create)))");
		assertNull(eval(ctx,"(call [nft t2] (get-metadata))"));
		assertNull(eval(ctx,"(call nft (get-metadata t2))"));

		// Burning NFT should delete metadata
		ctx=step(ctx,"(call nft (burn t1))");
		assertNotError(ctx);
		assertNull(eval(ctx,"(call [nft t1] (get-metadata))"));
		
	}
	
	@SuppressWarnings("unchecked")
	@Test public void testAssetAPI() {
		Context ctx=context();
		ctx=step(ctx,"(def total (map (fn [v] (call nft (create))) [1 2 3 4]))");
		AVector<CVMLong> v=(AVector<CVMLong>) ctx.getResult();
		assertEquals(4,v.count());
		CVMLong b1=v.get(0);
		
		// Test balance
		assertEquals(Sets.of(v.toCellArray()),eval(ctx,"(asset/balance nft)"));
		
		// Create test Users
		ctx=ctx.createAccount(KP1.getAccountKey());
		Address user1=(Address) ctx.getResult();
		ctx=ctx.createAccount(KP2.getAccountKey());
		Address user2=(Address) ctx.getResult();
		
		ctx=step(ctx,"(asset/transfer "+user1+" [nft (set (next total))])");
		ctx=step(ctx,"(asset/transfer "+user2+" [nft #{"+b1+"}])");
		assertEquals(Sets.of(b1),ctx.getResult());
		
		AssetTester.doAssetTests(ctx, NFT, user1, user2);
	}
}
