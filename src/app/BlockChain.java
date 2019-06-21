package app;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {

	public static ArrayList<Block> blockchain = new ArrayList<Block>();
	public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();

	public static int difficulty = 3;
	public static float minimumTransaction = 0.1f;
	public static Wallet Alice;
	public static Wallet Bob;
	public static Transaction genesisTransaction;

	public static void main(String[] args) {
		// 보안 제공자로 Bouncey castle 설정:
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		// 지갑 생성:
		Alice = new Wallet();
		Bob = new Wallet();
		Wallet coinbase = new Wallet();

		// 제네시스 트랜잭션 생성, Alice에게 100BTC 생성:
		genesisTransaction = new Transaction(coinbase.publicKey, Alice.publicKey, 100f, null);
		genesisTransaction.generateSignature(coinbase.privateKey); // 제네시스 트랜잭션 서명 생성
		genesisTransaction.transactionId = "0"; // 트랜잭션 아이디 설정
		genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value,
				genesisTransaction.transactionId)); // 트랜잭션 출력값 추가
		UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); // UTXOs 리스트에 첫번째 트랜잭션 저장

		// 제네시스 블럭 생성
		System.out.println("제네시스 블록 생성 및 마이닝\n");
		Block genesis = new Block("0");
		genesis.addTransaction(genesisTransaction);
		addBlock(genesis);

		// 블럭1 생성, Alice -> Bob 40BTC 지불
		Block block1 = new Block(genesis.hash);
		System.out.println("\n앨리스 잔액: " + Alice.getBalance());
		System.out.println("\n앨리스가 밥에게 40BTC 전송");
		block1.addTransaction(Alice.sendFunds(Bob.publicKey, 40f));
		addBlock(block1);
		System.out.println("\n앨리스 잔액: " + Alice.getBalance());
		System.out.println("밥 잔액: " + Bob.getBalance());

		// 블럭2 생성, Alice -> Bob 1000BTC 지불
		// * Alice의 코인이 100 뿐이므로 거래실패
		Block block2 = new Block(block1.hash);
		System.out.println("\n앨리스가 밥에게 1000BTC 전송");
		block2.addTransaction(Alice.sendFunds(Bob.publicKey, 1000f));
		addBlock(block2);
		System.out.println("\n앨리스 잔액: " + Alice.getBalance());
		System.out.println("밥 잔액: " + Bob.getBalance());

		// 블럭3 생성, Bob -> Alice 20BTC 지불
		Block block3 = new Block(block2.hash);
		System.out.println("\n밥이 앨리스에게 20BTC 전송");
		block3.addTransaction(Bob.sendFunds(Alice.publicKey, 20));
		addBlock(block3);
		System.out.println("\n앨리스 잔액: " + Alice.getBalance());
		System.out.println("밥 잔액: " + Bob.getBalance());

		// 블록체인이 유효한지 확인
		isChainValid();
	}

	// 블록체인 유효성 체크 메서드 POW(작업증명)
	public static Boolean isChainValid() {
		Block currentBlock;
		Block previousBlock;
		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		HashMap<String, TransactionOutput> tempUTXOs = new HashMap<String, TransactionOutput>(); // 임시 UTXOs 리스트

		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

		// 블록체인을 돌며 해시값 체크:
		for (int i = 1; i < blockchain.size(); i++) {

			currentBlock = blockchain.get(i);
			previousBlock = blockchain.get(i - 1);

			// 계산된 해시와 등록된 해시 값 비교:
			if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
				System.err.println("현재 해시값들이 일치하지 않습니다.");
				return false;
			}

			// 이전 해시와 등록된 이전 해시 값 비교:
			if (!previousBlock.hash.equals(currentBlock.previousHash)) {
				System.err.println("이전 해시값들이 일치하지 않습니다.");
				return false;
			}

			// 해시가 마이닝 되었는지 확인
			if (!currentBlock.hash.substring(0, difficulty).equals(hashTarget)) {
				System.err.println("이 블럭은 마이닝 되지 않았습니다.");
				return false;
			}

			// 블록체인 트랜잭션 루프
			TransactionOutput tempOutput;
			for (int t = 0; t < currentBlock.transactions.size(); t++) {
				Transaction currentTransaction = currentBlock.transactions.get(t);

				// 서명 검증
				if (!currentTransaction.verifySignature()) {
					System.err.println("트랜잭션(" + t + ") 의 서명이 유효하지 않습니다.");
					return false;
				}

				// 현재 트랜잭션의 입력값과 출력값 비교
				if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.err.println("트랙재션(" + t + ")의 결과값이 입력값과 일치하지 않습니다.");
					return false;
				}

				// 현재 트랜잭션의 입력값 루프
				for (TransactionInput input : currentTransaction.inputs) {
					tempOutput = tempUTXOs.get(input.transactionOutputId);

					if (tempOutput == null) {
						System.err.println("트랙잭션(" + t + ")의 참조된 입력값이 없습니다.");
						return false;
					}

					if (input.UTXO.value != tempOutput.value) {
						System.err.println("트랜잭션(" + t + ")의 참조된 입력값이 유효하지 않습니다.");
						return false;
					}

					tempUTXOs.remove(input.transactionOutputId);
				}

				// 현재 트랜잭션의 출력값 루프
				for (TransactionOutput output : currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}

				// 트랜잭션의 수신자가 위조되었는지 확인
				if (currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
					System.err.println("트랜재션(" + t + ") 출력값의 수신자가 위조되었습니다.");
					return false;
				}

				// 트랜잭션의 송신자가 위조되었는지 확인
				if (currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
					System.err.println("트랜잭션(" + t + ") 출력값의 송신자가 위조되었습니다.");
					return false;
				}

			}

		}

		System.out.println("블록이 유효합니다.");
		return true;
	}

	// 블록 추가 메서드
	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty); // 블록 마이닝
		blockchain.add(newBlock); // 새 블록 추가
	}
}