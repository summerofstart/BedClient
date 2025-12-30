"use client";

import { useState, useEffect } from 'react';

interface BedwarsStats {
  wins_bedwars?: number;
  losses_bedwars?: number;
  final_kills_bedwars?: number;
  final_deaths_bedwars?: number;
}

interface PlayerData {
  uuid: string;
  username: string;
  stats: BedwarsStats;
}

const Home = () => {
  const [players, setPlayers] = useState<PlayerData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPlayers = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/live');
      if (!response.ok) {
        throw new Error('Network response was not ok');
      }
      const data = await response.json();
      setPlayers(data);
      setError(null);
    } catch (error) {
      setError('Failed to fetch player data.');
      console.error('There was an error fetching the player data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlayers();
    const interval = setInterval(fetchPlayers, 5000); // 5秒ごとにデータを更新

    return () => clearInterval(interval);
  }, []);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24 bg-gray-900 text-white">
      <div className="z-10 w-full max-w-5xl items-center justify-between font-mono text-sm lg:flex">
        <h1 className="text-4xl font-bold mb-8">Live Bedwars Players</h1>
      </div>

      {loading && <p>Loading...</p>}
      {error && <p className="text-red-500">{error}</p>}

      {!loading && !error && (
        <div className="relative overflow-x-auto shadow-md sm:rounded-lg w-full">
          <table className="w-full text-sm text-left text-gray-400">
            <thead className="text-xs uppercase bg-gray-700 text-gray-400">
              <tr>
                <th scope="col" className="px-6 py-3">Player</th>
                <th scope="col" className="px-6 py-3">FKDR</th>
                <th scope="col" className="px-6 py-3">WLR</th>
                <th scope="col" className="px-6 py-3">Wins</th>
                <th scope="col" className="px-6 py-3">Final Kills</th>
              </tr>
            </thead>
            <tbody>
              {players.length > 0 ? (
                players.map((player) => (
                  <tr key={player.uuid} className="border-b bg-gray-800 border-gray-700 hover:bg-gray-600">
                    <th scope="row" className="px-6 py-4 font-medium whitespace-nowrap text-white">
                      {player.username}
                    </th>
                    <td className="px-6 py-4">
                      {(player.stats?.final_kills_bedwars && player.stats?.final_deaths_bedwars)
                        ? (player.stats.final_kills_bedwars / player.stats.final_deaths_bedwars).toFixed(2)
                        : 'N/A'}
                    </td>
                    <td className="px-6 py-4">
                      {(player.stats?.wins_bedwars && player.stats?.losses_bedwars)
                        ? (player.stats.wins_bedwars / player.stats.losses_bedwars).toFixed(2)
                        : 'N/A'}
                    </td>
                    <td className="px-6 py-4">{player.stats?.wins_bedwars ?? 'N/A'}</td>
                    <td className="px-6 py-4">{player.stats?.final_kills_bedwars ?? 'N/A'}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="px-6 py-4 text-center">No players are currently in a game.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
};

export default Home;
