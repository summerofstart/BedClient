import { NextRequest, NextResponse } from 'next/server';

// プレイヤーデータを保存するためのシンプルなインメモリキャッシュ
// TODO: 本番環境では、より永続的なストレージ（データベースなど）を検討する必要があります。
interface PlayerData {
  uuid: string;
  username: string;
  stats: any; // Hypixel APIから取得した統計情報
  lastUpdate: number; // 最終更新タイムスタンプ
}

let playerDataStore: PlayerData[] = [];

// プレイヤーデータを追加または更新する
const updatePlayerData = (data: PlayerData) => {
  const now = Date.now();
  const existingPlayerIndex = playerDataStore.findIndex(p => p.uuid === data.uuid);

  if (existingPlayerIndex !== -1) {
    // プレイヤーが既に存在する場合は、データを更新
    playerDataStore[existingPlayerIndex] = { ...data, lastUpdate: now };
  } else {
    // 新しいプレイヤーを追加
    playerDataStore.push({ ...data, lastUpdate: now });
  }

  // 古いデータをクリーンアップ（例：5分以上更新がないプレイヤーは削除）
  const fiveMinutesAgo = now - 5 * 60 * 1000;
  playerDataStore = playerDataStore.filter(p => p.lastUpdate > fiveMinutesAgo);
};


/**
 * MODからプレイヤーデータを受信するためのPOSTリクエストを処理します。
 * @param req - NextRequestオブジェクト
 * @returns - NextResponseオブジェクト
 */
export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    // 簡単なバリデーション
    if (!body.uuid || !body.username || !body.stats) {
      return NextResponse.json({ message: 'Invalid data format' }, { status: 400 });
    }

    // データをインメモリキャッシュに保存
    updatePlayerData(body);

    return NextResponse.json({ message: 'Player data received successfully' }, { status: 200 });
  } catch (error) {
    console.error('Error processing POST request:', error);
    return NextResponse.json({ message: 'Internal Server Error' }, { status: 500 });
  }
}

/**
 * フロントエンドにプレイヤーデータのリストを提供するためのGETリクエストを処理します。
 * @returns - NextResponseオブジェクト（プレイヤーデータの配列を含む）
 */
export async function GET() {
  try {
    // 現在保存されているプレイヤーデータのリストを返す
    return NextResponse.json(playerDataStore, { status: 200 });
  } catch (error) {
    console.error('Error processing GET request:', error);
    return NextResponse.json({ message: 'Internal Server Error' }, { status: 500 });
  }
}
